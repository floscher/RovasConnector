// License: GPL. For details, see LICENSE file.
package app.rovas.josm.gui.upload;

import java.awt.Window;

import app.rovas.josm.model.TimeTrackingManager;
import app.rovas.josm.util.UrlProvider;

/**
 * Contains the common parts of the three upload steps that make the API queries.
 */
public interface UploadStep {

  /**
   * Starts the upload step. The message will block until the step is done.
   */
  void showStep(
    final Window parent,
    final UrlProvider urlProvider,
    final TimeTrackingManager timeTrackingManager
  );
}
