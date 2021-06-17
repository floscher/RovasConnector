// License: GPL. For details, see LICENSE file.
package app.rovas.josm.gui.upload;

import java.awt.Window;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.api.ApiQuery;
import app.rovas.josm.gui.CreateRovasReportDialog;
import app.rovas.josm.util.UrlProvider;

/**
 * Contains the common parts of the three upload steps that make the API queries.
 */
public abstract class UploadStep {
  protected static final int MAX_STEP_REPETITIONS = 5;

  protected final Window parent;
  protected final UrlProvider urlProvider;

  /**
   * Create a new step using
   * @param parent the parent dialog (the {@link CreateRovasReportDialog})
   * @param urlProvider the URL provider from which we can get the URLs to call
   */
  public UploadStep(final Window parent, final UrlProvider urlProvider) {
    this.parent = parent;
    this.urlProvider = urlProvider;
  }

  /**
   * Starts the upload step. The message will block until the step is done.
   */
  public abstract void showStep();

  /**
   * Displays a {@link JOptionPane} as a warning with an error message
   * @param errorCode the error code with the message to show
   */
  protected void showErrorMessage(final ApiQuery.ErrorCode errorCode) {
    JOptionPane.showMessageDialog(
      parent,
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
