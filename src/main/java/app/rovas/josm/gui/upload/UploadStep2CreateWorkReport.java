package app.rovas.josm.gui.upload;

import java.awt.Window;
import java.util.Optional;

import org.openstreetmap.josm.data.osm.Changeset;

import app.rovas.josm.api.ApiCreateWorkReport;
import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.util.UrlProvider;

/**
 * The second upload step, which creates the work report.
 */
public class UploadStep2CreateWorkReport extends UploadStep {
  private final ApiCredentials credentials;
  private final int minutes;
  private final Optional<Changeset> changeset;

  public UploadStep2CreateWorkReport(
    final Window parent,
    final UrlProvider urlProvider,
    final ApiCredentials credentials,
    final int minutes,
    final Optional<Changeset> changeset
  ) {
    super(parent, urlProvider);
    this.credentials = credentials;
    this.minutes = minutes;
    this.changeset = changeset;
  }

  @Override
  public void showStep() {
    parent.setVisible(false);

    new ApiCreateWorkReport(urlProvider, minutes, changeset).query(
      credentials,
      workReportId -> {
        new UploadStep3CreateAur(parent, urlProvider, credentials, workReportId, minutes).showStep();
      },
      errorCode -> {
        showErrorMessage(errorCode);
        if (errorCode.getContinueOption() == ApiCreateWorkReport.ErrorCode.ContinueOption.CONTINUE_TO_AUR_QUERY) {
          new UploadStep3CreateAur(parent, urlProvider, credentials, 0, minutes).showStep();
        } else {
          parent.setVisible(true);
        }
      }
    );
  }
}
