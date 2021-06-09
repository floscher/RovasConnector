package app.rovas.josm.api;

import java.net.URLConnection;
import java.util.Optional;
import javax.json.Json;

import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.util.RovasProperties;
import app.rovas.josm.util.UrlProvider;

public final class ApiCreateAur extends ApiQuery<ApiQuery.ErrorCode> {
  private final int workReportId;
  private final int reportedMinutes;

  public ApiCreateAur(final UrlProvider urlProvider, final int workReportId, final int reportedMinutes) {
    super(urlProvider, urlProvider.rulesCreateAUR());
    this.workReportId = workReportId;
    this.reportedMinutes = reportedMinutes;
  }

  @Override
  protected ErrorCode[] getKnownErrorCodes() {
    return new ErrorCode[0];
  }

  @Override
  protected ErrorCode createAdditionalErrorCode(final Optional<Integer> code, final String translatableMessage) {
    return new ErrorCode(code, translatableMessage);
  }

  @Override
  protected int query(final ApiCredentials credentials) throws ApiException {
    final URLConnection connection = sendPostRequest(
      credentials,
      Json.createObjectBuilder()
        .add("project_id", credentials.getProjectId())
        .add("wr_id", workReportId)
        .add("usage_fee", reportedMinutes / 6.0 * RovasProperties.ASSET_USAGE_FEE)
        .add("note", String.format("%.2f%% fee levied by the 'JOSM Rovas connector' project for using the plugin", RovasProperties.ASSET_USAGE_FEE * 100))
    );
    return decodeJsonResult(connection, "result");
  }
}
