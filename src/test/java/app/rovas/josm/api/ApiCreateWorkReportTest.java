package app.rovas.josm.api;

import static app.rovas.josm.api.ApiQueryTest.DEFAULT_CREDENTIALS;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
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

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.tools.Logging;

import app.rovas.josm.fixture.NoTimeConsumer;
import app.rovas.josm.fixture.OneTimeConsumer;
import app.rovas.josm.fixture.WiremockExtension;
import app.rovas.josm.util.UrlProvider;

@ExtendWith(WiremockExtension.class)
public class ApiCreateWorkReportTest {

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
      0.1,
      6,
      "https://overpass-api.de/achavi/?changeset=123",
      78910,
      0,
      ApiCreateWorkReport.ErrorCode.ContinueOption.CONTINUE_TO_AUR_QUERY,
      okJson("{\"created_wr_nid\":0}")
    );
    assertQueryError(
      server,
      urlProvider,
      123,
      0.5,
      30,
      "https://overpass-api.de/achavi/?changeset=123",
      78910,
      -1,
      ApiCreateWorkReport.ErrorCode.ContinueOption.SHOW_WORK_REPORT_DIALOG_AGAIN,
      okJson("{\"created_wr_nid\":-1}")
    );
    assertQueryError(
      server,
      urlProvider,
      123,
      0.5,
      30,
      "https://overpass-api.de/achavi/?changeset=123",
      78910,
      -2,
      ApiCreateWorkReport.ErrorCode.ContinueOption.SHOW_WORK_REPORT_DIALOG_AGAIN,
      okJson("{\"created_wr_nid\":-2}")
    );
    assertQueryError(
      server,
      urlProvider,
      1,
      0.0,
      0,
      "https://overpass-api.de/achavi/?changeset=1",
      0,
      -3,
      ApiCreateWorkReport.ErrorCode.ContinueOption.CONTINUE_TO_AUR_QUERY,
      okJson("{\"created_wr_nid\":-3}")
    );
  }


  @Test
  protected void testErrorUnknown(final WireMockServer server, final UrlProvider urlProvider) {
    assertQueryError(
      server,
      urlProvider,
      1,
      1.0,
      60,
      "https://overpass-api.de/achavi/?changeset=1",
      123,
      -4,
      ApiCreateWorkReport.ErrorCode.ContinueOption.SHOW_WORK_REPORT_DIALOG_AGAIN,
      okJson("{\"created_wr_nid\":-4}")
    );
    assertQueryError(
      server,
      urlProvider,
      2,
      2.0,
      120,
      "https://overpass-api.de/achavi/?changeset=2",
      123,
      Integer.MIN_VALUE,
      ApiCreateWorkReport.ErrorCode.ContinueOption.SHOW_WORK_REPORT_DIALOG_AGAIN,
      okJson("{\"created_wr_nid\":" + Integer.MIN_VALUE + "}")
    );
  }

  @Test
  protected void testNormalResponse(final WireMockServer server, final UrlProvider urlProvider) throws ApiException {
    final Changeset changeset = new Changeset(1729);
    changeset.setCreatedAt(Instant.ofEpochSecond(73));

    stubForCredentials(server, 0.7, "https://overpass-api.de/achavi/?changeset=1729", 73, okJson("{\"created_wr_nid\":12345}"));
    assertEquals(12345, new ApiCreateWorkReport(urlProvider, 42, Optional.of(changeset)).query(DEFAULT_CREDENTIALS));
    verifyOneRequestAndReset(server);
  }

  private void assertQueryError(
    final WireMockServer server,
    final UrlProvider urlProvider,
    final int changesetId,
    final double hours,
    final int minutes,
    final String webAddress,
    final long unixTimestamp,
    final int expectedErrorCode,
    final ApiCreateWorkReport.ErrorCode.ContinueOption continueOption,
    final ResponseDefinitionBuilder response
  ) {
    final Changeset changeset = new Changeset(changesetId);
    changeset.setCreatedAt(Instant.ofEpochSecond(unixTimestamp));

    stubForCredentials(server, hours, webAddress, unixTimestamp, response);
    final OneTimeConsumer<ApiCreateWorkReport.ErrorCode> errorConsumer = new OneTimeConsumer<>(
      new ApiCreateWorkReport.ErrorCode(Optional.of(expectedErrorCode), "", continueOption),
      (a, b) ->
        (a != null && b != null) &&
          Objects.equals(a.getCode(), b.getCode()) &&
          Objects.equals(a.getContinueOption(), b.getContinueOption())
    );
    new ApiCreateWorkReport(urlProvider, minutes, Optional.of(changeset)).query(DEFAULT_CREDENTIALS, new NoTimeConsumer<>(), errorConsumer);
    errorConsumer.assertHasAccepted();
    verifyOneRequestAndReset(server);
  }

  private static void verifyOneRequestAndReset(final WireMockServer server) {
    server.verify(exactly(1), postRequestedFor(urlEqualTo("/rovas/rules/rules_proxy_create_work_report")));
    server.resetRequests();
  }

  @SuppressWarnings("UnusedReturnValue")
  private static StubMapping stubForCredentials(
    final Stubbing server,
    final double hours,
    final String webAddress,
    final long unixTimestamp,
    final ResponseDefinitionBuilder response
  ) {
    return server.stubFor(
      post("/rovas/rules/rules_proxy_create_work_report")
        .withHeader("API-KEY", equalTo(DEFAULT_CREDENTIALS.getApiKey()))
        .withHeader("TOKEN", equalTo(DEFAULT_CREDENTIALS.getApiToken()))
        .withHeader("Content-Type", matching("application/json;.+"))
        .withRequestBody(
          equalToJson(
            "{" +
              "\"wr_classification\":1645," +
              "\"wr_description\":\"Made edits to the OpenStreetMap project. This report was created automatically by the <a href=\\\"https://wiki.openstreetmap.org/wiki/JOSM/Plugins/RovasConnector\\\">Rovas connector plugin for JOSM</a>\"," +
              "\"wr_activity_name\":\"Creating map data with JOSM\"," +
              "\"wr_hours\":" + hours + "," +
              "\"wr_web_address\":\"" + webAddress + "\"," +
              "\"parent_project_nid\":" + DEFAULT_CREDENTIALS.getProjectId() + "," +
              "\"date_started\":" + unixTimestamp + "," +
              // access token ignored, because that is random every time
              "\"publish_status\":1" +
              "}",
            false,
            true
          )
        )
        .willReturn(response)
    );
  }

}
