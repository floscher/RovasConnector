package app.rovas.josm.api;

import java.net.URLConnection;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import javax.json.Json;

import com.drew.lang.annotations.NotNull;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.model.StaticConfig;
import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.util.UrlProvider;

public final class ApiCreateWorkReport extends ApiQuery<ApiCreateWorkReport.ErrorCode> {

  private final double minutes;
  private final Optional<Changeset> changeset;

  public ApiCreateWorkReport(final UrlProvider urlProvider, final int minutes, @NotNull final Optional<Changeset> changeset) {
    super(urlProvider, urlProvider.rulesCreateWorkReport());
    this.minutes = minutes;
    this.changeset = changeset;
  }

  @Override
  protected ErrorCode[] getKnownErrorCodes() {
    return new ErrorCode[]{
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
  }

  @Override
  protected ErrorCode createAdditionalErrorCode(final Optional<Integer> code, final String translatableMessage) {
    return new ErrorCode(code, translatableMessage, ErrorCode.ContinueOption.SHOW_WORK_REPORT_DIALOG_AGAIN);
  }

  @Override
  protected int query(final ApiCredentials credentials) throws ApiException {
    final SecureRandom random = new SecureRandom();
    final byte[] accessToken = new byte[12]; // 12 bytes → 16 Base64 characters
    random.nextBytes(accessToken);

    final URLConnection connection = sendPostRequest(
      credentials,
      Json.createObjectBuilder()
        .add("wr_classification", StaticConfig.NACE_CLASSIFICATION)
        .add("wr_description", I18n.tr(
          // i18n: {0} will be replaced by a link labeled: "Rovas connector plugin for JOSM"
          "Made edits to the OpenStreetMap project. This report was created automatically by the {0}",
          UrlProvider.toHtmlHyperlink(urlProvider.osmWikiPluginArticle(), I18n.tr("Rovas connector plugin for JOSM"))
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
    return decodeJsonResult(connection, "created_wr_nid");
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
       * Shows the original dialog again, so the user can try again
       */
      SHOW_WORK_REPORT_DIALOG_AGAIN,
      /**
       * Continue with the AUR query anyway, as if the work report was created successfully.
       */
      CONTINUE_TO_AUR_QUERY
    }

    @NotNull
    private final ContinueOption continueOption;

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
