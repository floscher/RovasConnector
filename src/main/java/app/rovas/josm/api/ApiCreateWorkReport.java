// License: GPL. For details, see LICENSE file.
package app.rovas.josm.api;

import java.net.URLConnection;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.json.Json;

import com.drew.lang.annotations.NotNull;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.gen.BuildInfo;
import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.model.StaticConfig;
import app.rovas.josm.util.JsonUtil;
import app.rovas.josm.util.UrlProvider;

/**
 * API query for creating a work report
 */
public final class ApiCreateWorkReport extends ApiQuery<Integer, ApiCreateWorkReport.ErrorCode> {
  private static final SecureRandom RANDOM = new SecureRandom();

  private final double minutes;
  private final Optional<Changeset> changeset;

  /**
   * Creates a new query to the API endpoint for creating work reports
   * @param urlProvider the URL provider from which we get the URL
   * @param minutes the number of minutes that should be reported
   * @param changeset the changeset for which this work report is created, can be empty if there is no changeset
   */
  public ApiCreateWorkReport(final UrlProvider urlProvider, final int minutes, @NotNull final Optional<Changeset> changeset) {
    super(urlProvider, urlProvider.rulesCreateWorkReport());
    this.minutes = minutes;
    this.changeset = changeset;
  }

  private static final ErrorCode[] KNOWN_CODES = new ErrorCode[]{
    new ErrorCode(
      Optional.of(0),
      // This error is not expected to happen. The report is always marked as published!
      I18n.marktr("The report is unpublished and no verifiers were invited!"),
      ErrorCode.ContinueOption.CONTINUE_TO_AUR_QUERY
    ),
    new ErrorCode(
      Optional.of(-1),
      I18n.marktr("You are not a shareholder in the project with ID set in the preferences!"),
      ErrorCode.ContinueOption.SHOW_WORK_REPORT_DIALOG_AGAIN
    ),
      new ErrorCode(
        Optional.of(-2),
        I18n.marktr("The report you are trying to create has the `date_started` earlier than the date when you registered for Rovas!"),
        ErrorCode.ContinueOption.SHOW_WORK_REPORT_DIALOG_AGAIN
      ),
      new ErrorCode(
        Optional.of(-3),
        I18n.marktr("A work report was created, but no verifiers were invited as you have outstanding reports to verify. Please login to Rovas and verify the reports!"),
        ErrorCode.ContinueOption.CONTINUE_TO_AUR_QUERY
      ),
  };


  @NotNull
  @Override
  protected Optional<ErrorCode> getErrorCodeForResult(@NotNull Integer result) {
    if (result > 0) {
      return Optional.empty();
    }
    return Optional.of(
      Stream.of(KNOWN_CODES)
        .filter(it -> it.getCode().isPresent() && it.getCode().get().equals(result))
        .findFirst()
        .orElse(new ErrorCode(Optional.of(result), I18n.marktr("An unknown error occured (code=" + result + ")!"), ErrorCode.ContinueOption.SHOW_WORK_REPORT_DIALOG_AGAIN))
    );
  }

  @NotNull
  @Override
  protected ErrorCode getErrorCodeForException(@NotNull ApiException exception) {
    return new ErrorCode(
      Optional.empty(),
      exception.getMessage(),
      exception instanceof ApiException.DecodeResponse || exception instanceof ApiException.ConnectionFailure
        ? ErrorCode.ContinueOption.CONTINUE_TO_AUR_QUERY
        : ErrorCode.ContinueOption.SHOW_WORK_REPORT_DIALOG_AGAIN
    );
  }

  @Override
  protected String getQueryLabel() {
    return "create work report";
  }

  @Override
  protected Integer query(final ApiCredentials credentials) throws ApiException {
    final byte[] accessToken = new byte[12]; // 12 bytes ??? 16 Base64 characters
    RANDOM.nextBytes(accessToken);

    final URLConnection connection = sendPostRequest(
      credentials,
      Json.createObjectBuilder()
        .add("wr_classification", StaticConfig.NACE_CLASSIFICATION)
        .add("wr_description", I18n.tr(
          // i18n: {0} will be replaced by a link labeled: "Rovas connector plugin for JOSM"
          "Made edits to the OpenStreetMap project. This report was created automatically by the {0}",
          UrlProvider.toHtmlHyperlink(
            BuildInfo.OSM_WIKI_URL,
            // i18n: link label, will be inserted into: "This report was created automatically by the {0}"
            I18n.tr("Rovas connector plugin for JOSM")
          )
        ))
        .add("wr_activity_name", I18n.tr("Creating map data with JOSM"))
        .add("wr_hours", minutes / 60.0)
        .add(
          "wr_web_address",
          changeset
            .map(Changeset::getId)
            .map(it -> String.format(StaticConfig.ROVAS_PROOF_URL, it))
            .orElse("")
        )
        .add("parent_project_nid", credentials.getProjectId())
        .add(
          "date_started",
          changeset
            .map(Changeset::getCreatedAt)
            .map(Instant::getEpochSecond)
            .orElse(Instant.now().getEpochSecond())
        )
        .add("access_token", Base64.getEncoder().encodeToString(accessToken))
        .add("publish_status", 1)
    );
    return decodeJsonResult(connection, it -> JsonUtil.extractResponseCode(it, "created_wr_nid"));
  }

  /**
   * An error code that the API call can return
   */
  public static class ErrorCode extends ApiQuery.ErrorCode {
    /**
     * The possible options on how to continue after an error happened
     */
    public enum ContinueOption {
      /**
       * Shows the original dialog again, so the user can try again. No AUR is created.
       */
      SHOW_WORK_REPORT_DIALOG_AGAIN,
      /**
       * Continue with the AUR query anyway, as if the work report was created successfully.
       */
      CONTINUE_TO_AUR_QUERY
    }

    @NotNull
    private final ContinueOption continueOption;

    /**
     * Creates a new error code for the work report
     * @param code the code number of the error, or leave empty for errors unknown to the server
     * @param translatableMessage a message describing the error, that can be passed into {@link I18n#tr}
     * @param continueOption one of {@link ContinueOption}, never null
     */
    public ErrorCode(@NotNull final Optional<Integer> code, @NotNull final String translatableMessage, @NotNull final ContinueOption continueOption) {
      super(code, translatableMessage);
      this.continueOption = Objects.requireNonNull(continueOption);
    }

    @NotNull
    public ContinueOption getContinueOption() {
      return continueOption;
    }

    @Override
    public String toString() {
      return super.toString() + " (continue with " + continueOption.name() + ')';
    }
  }
}
