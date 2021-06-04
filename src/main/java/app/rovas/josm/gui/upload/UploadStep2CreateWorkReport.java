package app.rovas.josm.gui.upload;

import java.awt.Window;
import java.util.Optional;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.api.ApiCreateWorkReport;
import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.util.UrlProvider;

public class UploadStep2CreateWorkReport extends UploadStep {
  private final ApiCredentials credentials;
  private final double hours;
  private final Optional<Changeset> changeset;

  public UploadStep2CreateWorkReport(
    final Window parent,
    final UrlProvider urlProvider,
    final ApiCredentials credentials,
    final double hours,
    final Optional<Changeset> changeset
  ) {
    super(parent, urlProvider);
    this.credentials = credentials;
    this.hours = hours;
    this.changeset = changeset;
  }

  @Override
  public void showStep() {
    parent.setVisible(false);

    new ApiCreateWorkReport(urlProvider, hours, changeset).query(
      credentials,
      workReportId -> {
        JOptionPane.showMessageDialog(parent, workReportId, I18n.tr("Successful check"), JOptionPane.PLAIN_MESSAGE);
        parent.dispose();
      },
      errorCode -> {
        if (errorCode.getContinueOption() == ApiCreateWorkReport.ErrorCode.ContinueOption.CONTINUE_TO_AUR_QUERY) {
          JOptionPane.showConfirmDialog(
            parent,
            "Error occurred, but continue with third step! " + I18n.tr(errorCode.getTranslatableMessage())
          );
          parent.dispose();
        } else {
          JOptionPane.showMessageDialog(
            parent,
            I18n.tr(errorCode.getTranslatableMessage()),
            errorCode.getCode().map(it -> I18n.tr("Error {0}", it)).orElse(I18n.tr("Error")),
            JOptionPane.ERROR_MESSAGE
          );
        }
      }
    );
  }
}
