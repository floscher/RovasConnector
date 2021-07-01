// License: GPL. For details, see LICENSE file.
package app.rovas.josm.gui.upload;

import java.awt.Window;
import java.util.Objects;
import java.util.Optional;
import javax.swing.JOptionPane;

import com.drew.lang.annotations.NotNull;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.api.ApiCheckOrAddShareholder;
import app.rovas.josm.gui.ApiCredentialsPanel;
import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.model.RovasProperties;
import app.rovas.josm.model.TimeTrackingManager;
import app.rovas.josm.util.UrlProvider;

/**
 * The first upload step where it is just checked, if the user is a shareholder in the selected project,
 * or if they can be made a shareholder. In the latter case, they are immediately made a shareholder.
 */
public class UploadStep1AddShareholder implements UploadStep {

  protected static final int MAX_STEP_REPETITIONS = 5;

  private final int minutes;
  private final Optional<Changeset> changeset;

  /**
   * Creates the first upload step
   * @param minutes the number of minutes that will be reported to the server in the work report
   * @param changeset the changeset that should be associated with the work report, can be empty, but never null
   */
  public UploadStep1AddShareholder(
    final int minutes,
    @NotNull final Optional<Changeset> changeset
  ) {
    super();
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
    showStep(parent, urlProvider, timeTrackingManager, false, 0);
  }

  private void showStep(
    @NotNull final Optional<Window> parent,
    final UrlProvider urlProvider,
    final TimeTrackingManager timeTrackingManager,
    final boolean forceCredentialsDialog,
    final int recursionDepth
  ) {
    final Optional<ApiCredentials> initialCredentials = ApiCredentials.createFrom(
      RovasProperties.ROVAS_API_KEY.get(),
      RovasProperties.ROVAS_API_TOKEN.get(),
      RovasProperties.ACTIVE_PROJECT_ID.get()
    );

    final ApiCredentials credentials;
    if (!initialCredentials.isPresent() || forceCredentialsDialog) {
      final ApiCredentialsPanel apiCredentialsPanel = new ApiCredentialsPanel(true);
      Optional<ApiCredentials> newApiCredentials = Optional.empty();
      while (!newApiCredentials.isPresent()) {
        if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(MainApplication.getMainFrame(), apiCredentialsPanel, I18n.tr("Enter API credentials"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)) {
          newApiCredentials = ApiCredentials.createFrom(
            apiCredentialsPanel.getApiKeyValue(),
            apiCredentialsPanel.getApiTokenValue(),
            apiCredentialsPanel.getActiveProjectIdValue()
          );
        } else {
          // Go back to the main dialog
          parent.ifPresent(it -> it.setVisible(true));
          return;
        }
      }
      RovasProperties.persistApiCredentials(apiCredentialsPanel);
      credentials = newApiCredentials.get();
    } else {
      credentials = initialCredentials.get();
    }

    new ApiCheckOrAddShareholder(urlProvider).query(
      credentials,
      meritId -> new UploadStep2CreateWorkReport(credentials, minutes, changeset).showStep(parent, urlProvider, timeTrackingManager),
      errorCode -> {
        if (recursionDepth < MAX_STEP_REPETITIONS && JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
          parent.orElse(null),
          "<html>" +
            I18n.tr(errorCode.getTranslatableMessage()) +
            errorCode.getCode().map(code -> " " + I18n.tr("(error {0})", code)).orElse("") +
            "<br>" +
            I18n.tr("Do you want to modify the API credentials and then retry?") +
            "</html>",
          I18n.tr("Error. Retry?"),
          JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE
        )) {
          showStep(parent, urlProvider, timeTrackingManager, true, recursionDepth + 1);
        } else {
          parent.ifPresent(it -> it.setVisible(true));
        }
      }
    );
  }

}
