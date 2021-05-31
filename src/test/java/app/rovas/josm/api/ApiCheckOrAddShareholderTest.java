package app.rovas.josm.api;

import static app.rovas.josm.api.ApiCheckOrAddShareholder.ErrorCode;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Objects;
import java.util.Optional;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import app.rovas.josm.StaticConfig;
import app.rovas.josm.fixture.NoTimeConsumer;
import app.rovas.josm.fixture.OneTimeConsumer;
import app.rovas.josm.fixture.WiremockExtension;
import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.util.UrlProvider;

@ExtendWith(WiremockExtension.class)
public class ApiCheckOrAddShareholderTest {
  private static final ApiCredentials DEFAULT_CREDENTIALS = new ApiCredentials(
    "--------------------key--------------------",
    "-------------------token-------------------",
    42
  );

  @Test
  @DisplayName("Test cases that produce decode exceptions")
  public void testDecodeFaults(final WireMockServer server, final UrlProvider urlProvider) {
    assertQueryErrorWithApiException(
      server,
      urlProvider,
      ApiException.DecodeResponse.class,
      aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)
    );
  }

  @Test
  @DisplayName("Test cases that produce connection exceptions")
  public void testConnectionExceptions(final WireMockServer server, final UrlProvider urlProvider) {
    assertQueryErrorWithApiException(
      server,
      urlProvider,
      ApiException.ConnectionFailure.class,
      aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)
    );
    assertQueryErrorWithApiException(
      server,
      urlProvider,
      ApiException.ConnectionFailure.class,
      aResponse().withFault(Fault.EMPTY_RESPONSE)
    );
    assertQueryErrorWithApiException(
      server,
      urlProvider,
      ApiException.ConnectionFailure.class,
      aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)
    );
  }

  @Test
  @DisplayName("Test wrong plugin credentials (we expect the server to respond with a 401)")
  public void testInvalidPluginCredentials(final WireMockServer server, final UrlProvider urlProvider) {
    assertQueryErrorWithApiException(server, urlProvider, ApiException.WrongPluginApiCredentials.class,  unauthorized().withStatusMessage("Unauthorized : The API keys sent are invalid."));
  }

  @Test
  @DisplayName("the resulting JSON can use a JSON number instead of a JSON string, but no other types")
  public void testDifferentResultTypes(final WireMockServer server, final UrlProvider urlProvider) {
    assertQuerySuccess(server, urlProvider, 42, okJson("{\"result\": 42}").withStatusMessage("OK"));

    assertQueryErrorWithApiException(server, urlProvider, ApiException.DecodeResponse.class, okJson("{\"RESULT\": \"15\"}"));
    assertQueryErrorWithApiException(server, urlProvider, ApiException.DecodeResponse.class, okJson("{\"result\": []}"));
    assertQueryErrorWithApiException(server, urlProvider, ApiException.DecodeResponse.class, okJson("{\"result\":null}"));
  }

  @Test
  @DisplayName("error code -2 (expected if the user's API-key and API-token are invalid)")
  public void testErrorInvalidCredentials(final WireMockServer server, final UrlProvider urlProvider) {
    assertQueryError(
      server,
      urlProvider,
      -2,
      ErrorCode.ContinueOption.RETRY_UPDATE_API_CREDENTIALS,
      okJson("{\"result\":\"-2\"}").withStatusMessage("OK")
    );
  }

  @Test
  @DisplayName("error code -1 (expected if the requested node ID is not referring to a project)")
  public void testErrorNotAProject(final WireMockServer server, final UrlProvider urlProvider) {
    assertQueryError(
      server,
      urlProvider,
      -1,
      ErrorCode.ContinueOption.RETRY_UPDATE_API_CREDENTIALS,
      okJson("{\"result\":\"-1\"}").withStatusMessage("OK")
    );
  }

  @Test
  @DisplayName("error code 0 (expected if the provided node ID belongs to a private project)")
  public void testErrorProjectPrivate(final WireMockServer server, final UrlProvider urlProvider) {
    assertQueryError(
      server,
      urlProvider,
      0,
      ErrorCode.ContinueOption.RETRY_UPDATE_API_CREDENTIALS,
      okJson("{\"result\":\"0\"}").withStatusMessage("OK")
    );
  }

  @Test
  @DisplayName("unknown error codes")
  public void testUnknownError(final WireMockServer server, final UrlProvider urlProvider) {
    assertQueryError(
      server,
      urlProvider,
      -4,
      ErrorCode.ContinueOption.RETRY_IMMEDIATELY,
      okJson("{\"result\":\"-4\"}").withStatusMessage("OK")
    );
    assertQueryError(
      server,
      urlProvider,
      Integer.MIN_VALUE,
      ErrorCode.ContinueOption.RETRY_IMMEDIATELY,
      okJson("{\"result\":\"" + Integer.MIN_VALUE + "\"}").withStatusMessage("OK")
    );
  }

  @Test
  @DisplayName("successful queries, where the user was (made) shareholder for the requested node ID")
  public void testProjectFound(final WireMockServer server, final UrlProvider urlProvider) {
    assertQuerySuccess(server, urlProvider, 1, okJson("{\"result\":\"1\"}").withStatusMessage("OK"));
    assertQuerySuccess(server, urlProvider, 1234, okJson("{\"result\":\"1234\"}").withStatusMessage("OK"));
    assertQuerySuccess(server, urlProvider, Integer.MAX_VALUE, okJson("{\"result\":\"" + Integer.MAX_VALUE + "\"}").withStatusMessage("OK"));
  }


  private void assertQueryError(
    final WireMockServer server,
    final UrlProvider urlProvider,
    final Integer expectedErrorCode,
    final ErrorCode.ContinueOption expectedContinueOption,
    final ResponseDefinitionBuilder response
  ) {
    stubForDefaultCredentials(server, response);
    final OneTimeConsumer<ErrorCode> errorConsumer = new OneTimeConsumer<>(
      new ErrorCode(Optional.ofNullable(expectedErrorCode), "", expectedContinueOption),
      (a, b) ->
        (a != null && b != null) &&
          Objects.equals(a.getCode(), b.getCode()) &&
          Objects.equals(a.getContinueOption(), b.getContinueOption()),
      it -> String.format(
        "ErrorCode{code=%s, continueOption=%s} (message: %s)",
        it.getCode().orElse(null),
        it.getContinueOption().name(),
        it.getTranslatableMessage()
      )
    );
    new ApiCheckOrAddShareholder(urlProvider).query(DEFAULT_CREDENTIALS, new NoTimeConsumer<>(), errorConsumer);
    errorConsumer.assertHasAccepted();
    verifyOneRequestAndReset(server);
  }

  private void assertQueryErrorWithApiException(
    final WireMockServer server,
    final UrlProvider urlProvider,
    final Class<? extends ApiException> expectedException,
    final ResponseDefinitionBuilder response
  ) {
    stubForDefaultCredentials(server, response);
    assertThrows(expectedException, () -> new ApiCheckOrAddShareholder(urlProvider).query(DEFAULT_CREDENTIALS));
    verifyOneRequestAndReset(server);

    assertQueryError(server, urlProvider, null, ErrorCode.ContinueOption.RETRY_IMMEDIATELY, response);
  }

  private void assertQuerySuccess(
    final WireMockServer server,
    final UrlProvider urlProvider,
    final int value,
    final ResponseDefinitionBuilder response
  ) {
    stubForDefaultCredentials(server, response);
    final OneTimeConsumer<Integer> successConsumer = new OneTimeConsumer<>(value);
    new ApiCheckOrAddShareholder(urlProvider).query(DEFAULT_CREDENTIALS, successConsumer, new NoTimeConsumer<>());
    successConsumer.assertHasAccepted();
    verifyOneRequestAndReset(server);
  }

  private static void verifyOneRequestAndReset(final WireMockServer server) {
    server.verify(exactly(1), postRequestedFor(urlEqualTo("/rovas/rules/rules_proxy_check_or_add_shareholder")));
    server.resetRequests();
  }

  @SuppressWarnings("UnusedReturnValue")
  private static StubMapping stubForDefaultCredentials(final Stubbing server, final ResponseDefinitionBuilder response) {
    return server.stubFor(
      post("/rovas/rules/rules_proxy_check_or_add_shareholder")
        .withHeader("API-KEY", equalTo(StaticConfig.PLUGIN_API_KEY))
        .withHeader("TOKEN", equalTo(StaticConfig.PLUGIN_API_TOKEN))
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(
          equalToJson(
            String.format(
              "{\"project_id\": %d,\"token\":\"%s\",\"api_key\":\"%s\"}",
              DEFAULT_CREDENTIALS.getProjectId(), DEFAULT_CREDENTIALS.getApiToken(), DEFAULT_CREDENTIALS.getApiKey()
            )
          )
        )
        .willReturn(response)
    );
  }

}
