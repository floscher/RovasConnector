package app.rovas.josm.gui.upload;

import java.awt.Window;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

import app.rovas.josm.api.ApiCreateAur;
import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.model.TimeTrackingManager;
import app.rovas.josm.util.UrlProvider;

/**
 * The third and last upload step, which creates the Asset Usage record (AUR) that accompanies the work report.
 */
public class UploadStep3CreateAur extends UploadStep {

  private final ApiCredentials credentials;
  private final int workReportId;
  private final int reportedMinutes;

  /**
   * Creates the third upload step for creating the AUR
   * @param parent the parent dialog
   * @param urlProvider the URL provider that supplies the URLs used
   * @param credentials the credentials used to authenticate with the API server
   * @param workReportId the ID of the work report for which an AUR should be created
   * @param reportedMinutes the number of minutes that were reported with the work report
   */
  public UploadStep3CreateAur(final Window parent, final UrlProvider urlProvider, final ApiCredentials credentials, final int workReportId, final int reportedMinutes) {
    super(parent, urlProvider);
    this.credentials = credentials;
    this.workReportId = workReportId;
    this.reportedMinutes = reportedMinutes;
  }

  @Override
  public void showStep() {
    TimeTrackingManager.getInstance().setCurrentlyTrackedSeconds(0);
    parent.dispose();
    new ApiCreateAur(urlProvider, workReportId, reportedMinutes).query(
      credentials,
      result -> {
        final HtmlPanel panel = new HtmlPanel(I18n.tr("Your {0} was created successfully!", UrlProvider.toHtmlHyperlink(UrlProvider.getInstance().node(workReportId), I18n.tr("work report"))));
        panel.setOpaque(false);
        panel.enableClickableHyperlinks();
        new Notification()
          .setContent(panel)
          .setIcon(ImageProvider.get("misc", "check_large"))
          .setDuration(Notification.TIME_LONG)
          .show();
      },
      errorCode -> JOptionPane.showMessageDialog(parent, I18n.tr("Failed to create AUR!"))
    );
  }
}
