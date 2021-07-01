// License: GPL. For details, see LICENSE file.
package app.rovas.josm.gui.upload;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.util.Optional;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import app.rovas.josm.fixture.WiremockExtension;
import app.rovas.josm.model.RovasProperties;
import app.rovas.josm.model.TimeTrackingManager;
import app.rovas.josm.util.UrlProvider;

@ExtendWith(WiremockExtension.class)
public class UploadTest {

  @Test
  @DisplayName("Test that the upload steps run through if all the steps are successful")
  protected void test(final Stubbing server, final UrlProvider urlProvider) {
    final TimeTrackingManager timeTrackingManager = new TimeTrackingManager();

    RovasProperties.ROVAS_API_KEY.put("abc");
    RovasProperties.ROVAS_API_TOKEN.put("def");
    RovasProperties.ACTIVE_PROJECT_ID.put(1729);

    server.stubFor(post("/rovas/rules/rules_proxy_check_or_add_shareholder").willReturn(okJson("{\"result\": 1234}")));
    server.stubFor(post("/rovas/rules/rules_proxy_create_work_report").willReturn(okJson("{\"created_wr_nid\": 5678}")));
    server.stubFor(post("/rovas/rules/rules_proxy_create_aur").willReturn(okJson("{\"result\": 91011}")));

    new UploadStep1AddShareholder(12, Optional.empty()).showStep(Optional.empty(), urlProvider, timeTrackingManager);

    server.verify(
      exactly(1),
      postRequestedFor(urlPathEqualTo("/rovas/rules/rules_proxy_check_or_add_shareholder"))
        .withRequestBody(equalToJson("{\"project_id\": 1729}"))
    );
    server.verify(
      exactly(1),
      postRequestedFor(urlPathEqualTo("/rovas/rules/rules_proxy_create_work_report"))
        .withRequestBody(equalToJson(
          "{\"wr_classification\" : 1645," +
          "\"wr_description\" : \"Made edits to the OpenStreetMap project. This report was created automatically by the <a href=\\\"https://wiki.openstreetmap.org/wiki/JOSM/Plugins/RovasConnector\\\">Rovas connector plugin for JOSM</a>\"," +
          "\"wr_activity_name\" : \"Creating map data with JOSM\"," +
          "\"wr_hours\" : 0.2,\"wr_web_address\" : \"\",\"parent_project_nid\" : 1729," +
          "\"publish_status\" : 1}",
          false,
          true
        ))
    );
    server.verify(
      exactly(1),
      postRequestedFor(urlPathEqualTo("/rovas/rules/rules_proxy_create_aur"))
        .withRequestBody(equalToJson(
          "{\"project_id\" : 35259,\"wr_id\" : 5678,\"usage_fee\" : 0.06," +
          "\"note\" : \"3.00% fee levied by the 'JOSM Rovas connector' project for using the plugin\"}"
        ))
    );
  }

  private MappingBuilder post(final String path) {
    return WireMock.post(path)
      .withHeader("API-KEY", equalTo("abc"))
      .withHeader("TOKEN", equalTo("def"))
      .withHeader("Content-Type", matching("application/json;.+"));
  }
}
