package app.rovas.josm.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import app.rovas.josm.fixture.WiremockExtension;
import app.rovas.josm.model.ApiCredentials;

@ExtendWith(WiremockExtension.class)
public class ApiCheckOrAddShareholderTest {

  @Test
  @DisplayName("Test if the correct exceptions are thrown for various faulty responses")
  public void testFaults(final WireMockServer server) {
    assertThrownException(server, aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK), ApiException.DecodeResponse.class);

    assertThrownException(server, aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE), ApiException.ConnectionFailure.class);
    assertThrownException(server, aResponse().withFault(Fault.EMPTY_RESPONSE), ApiException.ConnectionFailure.class);
    assertThrownException(server, aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER), ApiException.ConnectionFailure.class);
  }

  @Test
  public void testDifferentResultTypes(final WireMockServer server) throws ApiException.ConnectionFailure, ApiException.DecodeResponse {
    assertReturnValue(server, 42, 1729, okJson("{\"result\": 42}").withStatusMessage("OK"));

    assertThrownException(server, okJson("{\"RESULT\": \"15\"}"), ApiException.DecodeResponse.class);
    assertThrownException(server, okJson("{\"result\": []}"), ApiException.DecodeResponse.class);
    assertThrownException(server, okJson("{\"result\":null}"), ApiException.DecodeResponse.class);
  }

  @Test
  @DisplayName("Test if error code -2 is returned if API-key and API-token are invalid")
  public void testErrorWrongCredentials(final WireMockServer server) throws ApiException.ConnectionFailure, ApiException.DecodeResponse {
    assertReturnValue(
      server,
      ApiCheckOrAddShareholder.ERROR_CODE_SHAREHOLDER_UNAUTHORIZED,
      2,
      unauthorized().withStatusMessage("Unauthorized : The API keys sent are invalid.")
    );
  }

  @Test
  @DisplayName("Test if error code -1 is returned if the requested node ID is not referring to a project")
  public void testErrorNotAProject(final WireMockServer server) throws ApiException.ConnectionFailure, ApiException.DecodeResponse {
    assertReturnValue(
      server,
      ApiCheckOrAddShareholder.ERROR_CODE_SHAREHOLDER_NOT_FOUND,
      2,
      okJson("{\"result\":\"-1\"}").withStatusMessage("OK")
    );
  }

  @Test
  @DisplayName("Test if error code 0 is returned if the provided node ID belongs to a private project")
  public void testErrorProjectPrivate(final WireMockServer server) throws ApiException.ConnectionFailure, ApiException.DecodeResponse {
    assertReturnValue(
      server,
      ApiCheckOrAddShareholder.ERROR_CODE_SHAREHOLDER_PRIVATE,
      42,
      okJson("{\"result\":\"0\"}").withStatusMessage("OK")
    );
  }

  @Test
  @DisplayName("Test if a project ID is returned, if the user is (made) shareholder for the requested node ID")
  public void testErrorProjectFound(final WireMockServer server) throws ApiException.ConnectionFailure, ApiException.DecodeResponse {
    assertReturnValue(
      server,
      1235,
      1234,
      okJson("{\"result\":\"1235\"}").withStatusMessage("OK")
    );
  }

  private void assertReturnValue(
    final WireMockServer server,
    final int expectedReturnValue,
    final int requestedProjectId,
    final ResponseDefinitionBuilder response
  ) throws ApiException.ConnectionFailure, ApiException.DecodeResponse {
    assertReturnValue(server, expectedReturnValue, new ApiCredentials("myKey", "myToken", requestedProjectId), response);
  }

  private void assertReturnValue(
    final WireMockServer server,
    final int expectedReturnValue,
    final ApiCredentials credentials,
    final ResponseDefinitionBuilder response
  ) throws ApiException.ConnectionFailure, ApiException.DecodeResponse {
    stubForCredentials(server, credentials, response);
    assertEquals(expectedReturnValue, ApiCheckOrAddShareholder.query(credentials));
    server.verify(exactly(1), postRequestedFor(urlEqualTo("/rovas/rules/rules_proxy_check_or_add_shareholder")));
    server.resetRequests();
  }

  private static void assertThrownException(final WireMockServer server, final ResponseDefinitionBuilder response, final Class<? extends Throwable> exceptionType) {
    final ApiCredentials credentials = new ApiCredentials("key", "token", 42);
    stubForCredentials(server, credentials, response);
    assertThrows(exceptionType, () -> ApiCheckOrAddShareholder.query(credentials));
    server.verify(exactly(1), postRequestedFor(urlEqualTo("/rovas/rules/rules_proxy_check_or_add_shareholder")));
    server.resetRequests();
  }

  private static StubMapping stubForCredentials(final Stubbing server, final ApiCredentials credentials, final ResponseDefinitionBuilder response) {
    return server.stubFor(
      post("/rovas/rules/rules_proxy_check_or_add_shareholder")
        .withHeader("API-KEY", equalTo(credentials.getApiKey()))
        .withHeader("TOKEN", equalTo(credentials.getApiToken()))
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(
          equalToJson(
            String.format(
              "{\"project_id\": %d,\"token\":\"%s\",\"api_key\":\"%s\"}",
              credentials.getProjectId(), credentials.getApiToken(), credentials.getApiKey()
            )
          )
        )
        .willReturn(response)
    );
  }

}
