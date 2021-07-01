// License: GPL. For details, see LICENSE file.
package app.rovas.josm.gui.upload;

import java.awt.Window;
import java.util.Objects;
import java.util.Optional;
import javax.swing.JOptionPane;

import com.drew.lang.annotations.NotNull;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.api.ApiCreateWorkReport;
import app.rovas.josm.api.ApiQuery;
import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.model.TimeTrackingManager;
import app.rovas.josm.util.UrlProvider;

/**
 * The second upload step, which creates the work report.
 */
public class UploadStep2CreateWorkReport implements UploadStep {
  private final ApiCredentials credentials;
  private final int minutes;
  private final Optional<Changeset> changeset;

  /**
   * Creates the second upload step
   * @param credentials the credentials used to authenticate with the API
   * @param minutes the number of minutes that should be reported with tha work report
   * @param changeset the changeset that should be associated with the work report, can be empty but not null
   */
  public UploadStep2CreateWorkReport(
    final ApiCredentials credentials,
    final int minutes,
    final Optional<Changeset> changeset
  ) {
    super();
    this.credentials = Objects.requireNonNull(credentials);
    this.minutes = minutes;
    this.changeset = Objects.requireNonNull(changeset);
  }

  @Override
  public void showStep(
    @NotNull final Optional<Window> parent,
    @NotNull final UrlProvider urlProvider,
    @NotNull final TimeTrackingManager timeTrackingManager
  ) {
    parent.ifPresent(it -> it.setVisible(false));

    new ApiCreateWorkReport(urlProvider, minutes, changeset).query(
      credentials,
      workReportId -> {
        new UploadStep3CreateAur(credentials, workReportId, minutes).showStep(parent, urlProvider, timeTrackingManager);
      },
      errorCode -> {
        showErrorMessage(parent, errorCode);
        if (errorCode.getContinueOption() == ApiCreateWorkReport.ErrorCode.ContinueOption.CONTINUE_TO_AUR_QUERY) {
          new UploadStep3CreateAur(credentials, 0, minutes).showStep(parent, urlProvider, timeTrackingManager);
        } else {
          parent.ifPresent(it -> it.setVisible(true));
        }
      }
    );
  }

  /**
   * Displays a {@link JOptionPane} as a warning with an error message
   * @param errorCode the error code with the message to show
   */
  private void showErrorMessage(final Optional<Window> parent, final ApiQuery.ErrorCode errorCode) {
    JOptionPane.showMessageDialog(
      parent.orElse(null),
      I18n.tr(
        "An error occured!: {0}{1}",
        errorCode.getCode().map(it -> it + " ").orElse(""),
        I18n.tr(errorCode.getTranslatableMessage())
      ),
      I18n.tr("Error"),
      JOptionPane.ERROR_MESSAGE
    );
  }
}
