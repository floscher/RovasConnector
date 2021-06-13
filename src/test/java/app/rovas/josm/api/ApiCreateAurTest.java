package app.rovas.josm.api;

import static app.rovas.josm.api.ApiQueryTest.DEFAULT_CREDENTIALS;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.openstreetmap.josm.tools.Logging;

import app.rovas.josm.fixture.NoTimeConsumer;
import app.rovas.josm.fixture.OneTimeConsumer;
import app.rovas.josm.fixture.WiremockExtension;
import app.rovas.josm.model.StaticConfig;
import app.rovas.josm.util.TimeConverterUtil;
import app.rovas.josm.util.UrlProvider;

@ExtendWith(WiremockExtension.class)
public class ApiCreateAurTest {

  @BeforeAll
  public static void beforeAll() {
    Logging.setLogLevel(Level.FINEST);
  }

  @Test
  protected void testKnownErrors(final WireMockServer server, final UrlProvider urlProvider) {
    assertQueryError(
      server,
      urlProvider,
      123,
      101111,
      null,
      unauthorized()
    );
  }


  @Test
  protected void testErrorUnknown(final WireMockServer server, final UrlProvider urlProvider) {
    assertQueryError(
      server,
      urlProvider,
      1,
      1234,
      0,
      okJson("{\"result\":0}")
    );
    assertQueryError(
      server,
      urlProvider,
      2,
      123,
      Integer.MIN_VALUE,
      okJson("{\"result\":" + Integer.MIN_VALUE + "}")
    );
  }

  @Test
  protected void testNormalResponse(final WireMockServer server, final UrlProvider urlProvider) throws ApiException {
    stubForCredentials(server, 456, TimeConverterUtil.minutesToChrons(42) * StaticConfig.ASSET_USAGE_FEE, okJson("{\"result\":12345}"));
    final OneTimeConsumer<Integer> successConsumer = new OneTimeConsumer<>(12345);
    new ApiCreateAur(urlProvider, 456, 42).query(DEFAULT_CREDENTIALS, successConsumer, new NoTimeConsumer<>());
    successConsumer.assertHasAccepted();
    verifyOneRequestAndReset(server);
  }

  private void assertQueryError(
    final WireMockServer server,
    final UrlProvider urlProvider,
    final int workReportId,
    final int reportedMinutes,
    final Integer expectedErrorCode,
    final ResponseDefinitionBuilder response
  ) {
    stubForCredentials(server, workReportId, TimeConverterUtil.minutesToChrons(reportedMinutes) * StaticConfig.ASSET_USAGE_FEE, response);
    final OneTimeConsumer<ApiQuery.ErrorCode> errorConsumer = new OneTimeConsumer<>(
      new ApiQuery.ErrorCode(Optional.ofNullable(expectedErrorCode), ""),
      (a, b) ->
        (a != null && b != null) &&
          Objects.equals(a.getCode(), b.getCode())
    );
    new ApiCreateAur(urlProvider, workReportId, reportedMinutes).query(DEFAULT_CREDENTIALS, new NoTimeConsumer<>(), errorConsumer);
    errorConsumer.assertHasAccepted();
    verifyOneRequestAndReset(server);
  }

  private static void verifyOneRequestAndReset(final WireMockServer server) {
    server.verify(exactly(1), postRequestedFor(urlEqualTo("/rovas/rules/rules_proxy_create_aur")));
    server.resetRequests();
  }

  @SuppressWarnings("UnusedReturnValue")
  private static StubMapping stubForCredentials(
    final Stubbing server,
    final int workReportId,
    final double usageFee,
    final ResponseDefinitionBuilder response
  ) {
    return server.stubFor(
      post("/rovas/rules/rules_proxy_create_aur")
        .withHeader("API-KEY", equalTo(DEFAULT_CREDENTIALS.getApiKey()))
        .withHeader("TOKEN", equalTo(DEFAULT_CREDENTIALS.getApiToken()))
        .withHeader("Content-Type", matching("application/json;.+"))
        .withRequestBody(
          equalToJson(
            String.format(
              Locale.ROOT,
              "{\"project_id\":%d,\"wr_id\":%d,\"usage_fee\":%s," +
                "\"note\":\"%.2f%% fee levied by the 'JOSM Rovas connector' project for using the plugin\"}",
              StaticConfig.ROVAS_CONNECTOR_PROJECT_ID,
              workReportId,
              usageFee + "",
              StaticConfig.ASSET_USAGE_FEE * 100
            )
          )
        )
        .willReturn(response)
    );
  }

}
