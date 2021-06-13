package app.rovas.josm.api;

import static app.rovas.josm.api.ApiCheckOrAddShareholder.ErrorCode;
import static app.rovas.josm.api.ApiQueryTest.DEFAULT_CREDENTIALS;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
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

import app.rovas.josm.fixture.NoTimeConsumer;
import app.rovas.josm.fixture.OneTimeConsumer;
import app.rovas.josm.fixture.WiremockExtension;
import app.rovas.josm.util.UrlProvider;

@ExtendWith(WiremockExtension.class)
public class ApiCheckOrAddShareholderTest {

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
  @DisplayName("error code -1 (expected if no project is found for the given ID)")
  public void testErrorNotAProject(final WireMockServer server, final UrlProvider urlProvider) {
    assertQueryError(
      server,
      urlProvider,
      -1,
      okJson("{\"result\":\"-1\"}").withStatusMessage("OK")
    );
  }

  @Test
  @DisplayName("error code 0 (expected if the user can't access the project with the given ID)")
  public void testErrorProjectPrivate(final WireMockServer server, final UrlProvider urlProvider) {
    assertQueryError(
      server,
      urlProvider,
      0,
      okJson("{\"result\":\"0\"}").withStatusMessage("OK")
    );
  }

  @Test
  @DisplayName("unknown error codes")
  public void testUnknownError(final WireMockServer server, final UrlProvider urlProvider) {
    assertQueryError(
      server,
      urlProvider,
      -2,
      okJson("{\"result\":\"-2\"}").withStatusMessage("OK")
    );
    assertQueryError(
      server,
      urlProvider,
      Integer.MIN_VALUE,
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
    final ResponseDefinitionBuilder response
  ) {
    stubForCredentials(server, response);
    final OneTimeConsumer<ErrorCode> errorConsumer = new OneTimeConsumer<>(
      new ErrorCode(Optional.ofNullable(expectedErrorCode), ""),
      (a, b) ->
        (a != null && b != null) &&
        Objects.equals(a.getCode(), b.getCode())
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
    stubForCredentials(server, response);
    assertThrows(expectedException, () -> new ApiCheckOrAddShareholder(urlProvider).query(DEFAULT_CREDENTIALS));
    verifyOneRequestAndReset(server);

    assertQueryError(server, urlProvider, null, response);
  }

  private void assertQuerySuccess(
    final WireMockServer server,
    final UrlProvider urlProvider,
    final int value,
    final ResponseDefinitionBuilder response
  ) {
    stubForCredentials(server, response);
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
  private static StubMapping stubForCredentials(final Stubbing server, final ResponseDefinitionBuilder response) {
    return server.stubFor(
      post("/rovas/rules/rules_proxy_check_or_add_shareholder")
        .withHeader("API-KEY", equalTo(DEFAULT_CREDENTIALS.getApiKey()))
        .withHeader("TOKEN", equalTo(DEFAULT_CREDENTIALS.getApiToken()))
        .withHeader("Content-Type", matching("application/json;.+"))
        .withRequestBody(
          equalToJson(
            String.format(
              "{\"project_id\": %d}",
              DEFAULT_CREDENTIALS.getProjectId()
            )
          )
        )
        .willReturn(response)
    );
  }

}
