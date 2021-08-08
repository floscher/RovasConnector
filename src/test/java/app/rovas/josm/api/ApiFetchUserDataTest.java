package app.rovas.josm.api;

import static app.rovas.josm.api.ApiQueryTest.DEFAULT_CREDENTIALS;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.openstreetmap.josm.tools.Logging;

import app.rovas.josm.fixture.NoTimeConsumer;
import app.rovas.josm.fixture.OneTimeConsumer;
import app.rovas.josm.fixture.WiremockExtension;
import app.rovas.josm.util.UrlProvider;

@ExtendWith(WiremockExtension.class)
public class ApiFetchUserDataTest {

  @BeforeEach
  public void beforeAll() {
    Logging.setLogLevel(Level.FINEST);
  }

  @Test
  protected void testSuccess(final WireMockServer server, final UrlProvider urlProvider) throws ApiException {
    stubFor(server, okJson("{\"country\":\"DE\",\"uid\":42,\"username\":\"john.doe\",\"lang_code\":\"en\",\"email\":\"john@example.org\",\"comp_score\":\"5\",\"whole_name\":\"John Doe\"}"));

    final OneTimeConsumer<UserData> successCallback = new OneTimeConsumer<>(
      new UserData("DE", 42, "john.doe", "en", "john@example.org", "5", "John Doe"),
      (a, b) ->
        a.getComplianceScore() == b.getComplianceScore() &&
        Objects.equals(a.getCountry(), b.getCountry()) &&
        Objects.equals(a.getEmail(), b.getEmail()) &&
        Objects.equals(a.getLangCode(), b.getLangCode()) &&
        a.getUid() ==  b.getUid() &&
        Objects.equals(a.getUsername(), b.getUsername()) &&
        Objects.equals(a.getWholeName(), b.getWholeName())
    );
    final Consumer<ApiQuery.ErrorCode> errorCallback = new NoTimeConsumer<>();

    new ApiFetchUserData(urlProvider).query(DEFAULT_CREDENTIALS, successCallback, errorCallback);

    successCallback.assertHasAccepted();
    verifyOneRequestAndReset(server);
  }

  @Test
  protected void testError(final WireMockServer server, final UrlProvider urlProvider) throws ApiException {
    stubFor(server, unauthorized());

    assertThrows(ApiException.WrongPluginApiCredentials.class, () -> new ApiFetchUserData(urlProvider).query(DEFAULT_CREDENTIALS));

    verifyOneRequestAndReset(server);
  }
  private void stubFor(final WireMockServer server, ResponseDefinitionBuilder response) {
    server.stubFor(
      post("/rovas/rules/rules_fetch_user_data")
        .withHeader("API-KEY", equalTo(DEFAULT_CREDENTIALS.getApiKey()))
        .withHeader("TOKEN", equalTo(DEFAULT_CREDENTIALS.getApiToken()))
        .withHeader("Content-Type", matching("application/json;.+"))
        .withRequestBody(equalTo("{}"))
        .willReturn(response)
    );
  }

  private static void verifyOneRequestAndReset(final WireMockServer server) {
    server.verify(exactly(1), postRequestedFor(urlEqualTo("/rovas/rules/rules_fetch_user_data")));
    server.resetRequests();
  }
}
