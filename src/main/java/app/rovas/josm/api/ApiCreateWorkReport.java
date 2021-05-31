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

import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.util.RovasProperties;
import app.rovas.josm.util.UrlProvider;

public final class ApiCreateWorkReport extends ApiQuery<ApiCreateWorkReport.ErrorCode> {

  public static class ErrorCode extends ApiQuery.ErrorCode {
    public enum ContinueOption {
      SHOW_WORK_REPORT_DIALOG_AGAIN,
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
  }

  private final double hours;
  private final Changeset changeset;

  public ApiCreateWorkReport(final UrlProvider urlProvider, final double hours, @NotNull final Changeset changeset) {
    super(urlProvider, urlProvider.rules_createWorkReport());
    this.hours = hours;
    this.changeset = changeset;
  }

  @Override
  protected ErrorCode[] getErrorCodes() {
    return new ErrorCode[]{
      new ErrorCode(
        Optional.of(-1),
        I18n.marktr("You are not authorized to post reports in the set project!"),
        ErrorCode.ContinueOption.SHOW_WORK_REPORT_DIALOG_AGAIN
      ),
        new ErrorCode(
          Optional.of(-2),
          I18n.marktr("No user found for the supplied API key and token!"),
          ErrorCode.ContinueOption.SHOW_WORK_REPORT_DIALOG_AGAIN
        ),
        new ErrorCode(
          Optional.of(-3),
          I18n.marktr("A work report was created, but no verifiers were invited as you have outstanding reports to verify. Please login to Rovas and verify the reports!"),
          ErrorCode.ContinueOption.CONTINUE_TO_AUR_QUERY
        ),
        new ErrorCode(
          Optional.of(-4),
          I18n.marktr("A work report was not created, because the date of the report predates your Rovas registration date, which is against the rules."),
          ErrorCode.ContinueOption.SHOW_WORK_REPORT_DIALOG_AGAIN
        ),
    };
  }

  @Override
  protected ErrorCode createAdditionalErrorCode(Optional<Integer> code, String translatableMessage) {
    return new ErrorCode(code, translatableMessage, ErrorCode.ContinueOption.SHOW_WORK_REPORT_DIALOG_AGAIN);
  }

  @Override
  protected int query(final ApiCredentials credentials) throws ApiException {
    final SecureRandom random = new SecureRandom();
    final byte[] accessToken = new byte[12]; // 12 bytes → 16 Base64 characters
    random.nextBytes(accessToken);

    final URLConnection connection = sendPostRequest(
      Json.createObjectBuilder()
        .add("api_key", credentials.getApiKey())
        .add("token", credentials.getApiToken())
        .add("wr_classification", RovasProperties.NACE_CLASSIFICATION)
        .add("wr_description", I18n.tr(
          // i18n: {0} will be replaced by a link labeled: "Rovas connector plugin for JOSM"
          "Made edits to the OpenStreetMap project. This report was created automatically by the {0}",
          UrlProvider.toHtmlHyperlink(urlProvider.osmWikiPluginArticle(), I18n.tr("Rovas connector plugin for JOSM"))
        ))
        .add("wr_activity_name", I18n.tr("Creating map data with JOSM"))
        .add("wr_hours", hours)
        .add("wr_web_address", String.format(RovasProperties.ROVAS_PROOF_URL, changeset.getId()))
        .add("parent_project_nid", credentials.getProjectId())
        .add("date_started",
          Optional.ofNullable(changeset.getCreatedAt())
            .map(Instant::getEpochSecond)
            .orElse(Instant.now().getEpochSecond())
        )
        .add("access_token", Base64.getEncoder().encodeToString(accessToken))
        .add("publish_status", 1)
    );
    return decodeJsonResult(connection, "create_wr_id");
  }
}
