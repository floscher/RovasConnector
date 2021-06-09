package app.rovas.josm.gui.upload;

import java.awt.Window;

import javax.swing.JOptionPane;

import app.rovas.josm.api.ApiCreateAur;
import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.util.UrlProvider;

public class UploadStep3CreateAur extends UploadStep {

  private final ApiCredentials credentials;
  private final int workReportId;
  private final int reportedMinutes;

  public UploadStep3CreateAur(final Window parent, final UrlProvider urlProvider, final ApiCredentials credentials, final int workReportId, final int reportedMinutes) {
    super(parent, urlProvider);
    this.credentials = credentials;
    this.workReportId = workReportId;
    this.reportedMinutes = reportedMinutes;
  }

  @Override
  public void showStep() {
    new ApiCreateAur(urlProvider, workReportId, reportedMinutes).query(
      credentials,
      result -> {
        JOptionPane.showMessageDialog(parent, "Work report and AUR have been created successfully!");
      },
      errorCode -> {
        JOptionPane.showMessageDialog(parent, "Failed to create AUR!");
      }
    );
  }
}
