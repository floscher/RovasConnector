// License: GPL. For details, see LICENSE file.
package app.rovas.josm.api;

import java.net.URLConnection;
import java.util.Locale;
import java.util.Optional;
import javax.json.Json;

import com.drew.lang.annotations.NotNull;

import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.model.RovasProperties;
import app.rovas.josm.model.StaticConfig;
import app.rovas.josm.util.JsonUtil;
import app.rovas.josm.util.TimeConverterUtil;
import app.rovas.josm.util.UrlProvider;

/**
 * The API query that creates the Asset Usage Record (AUR) for the work report that has been created.
 */
public final class ApiCreateAur extends ApiQuery<Integer, ApiQuery.ErrorCode> {
  private final int workReportId;
  private final int reportedMinutes;

  /**
   * Create the query
   * @param urlProvider the {@link UrlProvider} from which we can obtain the API URL
   * @param workReportId the ID of the work report for which we create the AUR
   * @param reportedMinutes the number of minutes that were reported for the work report (for this amount we'll calculate the appropriate fee)
   */
  public ApiCreateAur(final UrlProvider urlProvider, final int workReportId, final int reportedMinutes) {
    super(urlProvider, urlProvider.rulesCreateAUR());
    this.workReportId = workReportId;
    this.reportedMinutes = reportedMinutes;
  }

  @NotNull
  @Override
  protected Optional<ErrorCode> getErrorCodeForResult(@NotNull Integer result) {
    if (result > 0) {
      return Optional.empty();
    }
    return Optional.of(new ErrorCode(Optional.of(result), I18n.marktr("An unknown error occured!")));
  }

  @NotNull
  @Override
  protected ErrorCode getErrorCodeForException(@NotNull ApiException exception) {
    return new ErrorCode(Optional.empty(), exception.getLocalizedMessage());
  }

  @Override
  protected String getQueryLabel() {
    return "create AUR";
  }

  @Override
  protected Integer query(final ApiCredentials credentials) throws ApiException {
    final URLConnection connection = sendPostRequest(
      credentials,
      Json.createObjectBuilder()
        .add(
          "project_id",
          RovasProperties.DEVELOPER.get()
            ? StaticConfig.ROVAS_CONNECTOR_PROJECT_ID_DEV
            : StaticConfig.ROVAS_CONNECTOR_PROJECT_ID
        )
        .add("wr_id", workReportId)
        .add("usage_fee", TimeConverterUtil.minutesToChrons(reportedMinutes) * StaticConfig.ASSET_USAGE_FEE)
        .add("note", I18n.tr("{0}% fee levied by the ''JOSM Rovas connector'' project for using the plugin", String.format(Locale.ROOT, "%.2f", StaticConfig.ASSET_USAGE_FEE * 100)))
    );
    return decodeJsonResult(connection, it -> JsonUtil.extractResponseCode(it, "result"));
  }
}
