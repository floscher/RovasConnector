package app.rovas.josm.gui.upload;

import java.awt.Window;
import java.util.Optional;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.api.ApiCheckOrAddShareholder;
import app.rovas.josm.gui.ApiCredentialsPanel;
import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.util.RovasProperties;
import app.rovas.josm.util.UrlProvider;

public class UploadStep1AddShareholder extends UploadStep {
  private static final int MAX_STEP_REPETITIONS = 5;

  public UploadStep1AddShareholder(final Window parent) {
    super(parent);
  }

  @Override
  public void showStep() {
    parent.setVisible(false);
    showStep(false, 0);
  }

  private void showStep(final boolean forceCredentialsDialog, final int recursionDepth) {
    final Optional<ApiCredentials> initialCredentials = RovasProperties.getApiCredentials();

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
          parent.setVisible(true);
          return;
        }
      }
      RovasProperties.persistApiCredentials(apiCredentialsPanel);
      credentials = newApiCredentials.get();
    } else {
      credentials = initialCredentials.get();
    }

    new ApiCheckOrAddShareholder(UrlProvider.getInstance()).query(
      credentials,
      meritId -> {
        JOptionPane.showMessageDialog(parent, meritId, I18n.tr("Successful check"), JOptionPane.PLAIN_MESSAGE);
        parent.dispose();
      },
      (errorCode) -> {
        final boolean forceCredentialsDialogAgain =
          ApiCheckOrAddShareholder.ErrorCode.ContinueOption.RETRY_UPDATE_API_CREDENTIALS == errorCode.getContinueOption();

        if (recursionDepth < MAX_STEP_REPETITIONS && JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
          parent,
          "<html>" +
            I18n.tr(errorCode.getTranslatableMessage()) + "<br>" +
            (
              forceCredentialsDialogAgain
              ? I18n.tr("Do you want to modify the API credentials and then retry?")
              : I18n.tr("Do you want to retry?")
            ) +
            "</html>",
          I18n.tr("Retry?"),
          JOptionPane.YES_NO_OPTION,
          forceCredentialsDialogAgain ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE
        )) {
          showStep(forceCredentialsDialogAgain, recursionDepth + 1);
        } else {
          parent.setVisible(true);
        }
      }
    );
  }

}
