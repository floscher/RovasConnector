package app.rovas.josm.gui.upload;

import java.awt.Window;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.api.ApiQuery;
import app.rovas.josm.util.UrlProvider;

public abstract class UploadStep {
  protected static final int MAX_STEP_REPETITIONS = 5;

  protected final Window parent;
  protected final UrlProvider urlProvider;

  public UploadStep(final Window parent, final UrlProvider urlProvider) {
    this.parent = parent;
    this.urlProvider = urlProvider;
  }

  public abstract void showStep();

  protected void showErrorMessage(final ApiQuery.ErrorCode errorCode) {
    JOptionPane.showMessageDialog(
      parent,
      I18n.tr("An error occured!: {0}", errorCode.getCode().map(it -> it + " ").orElse("") + I18n.tr(errorCode.getTranslatableMessage())),
      I18n.tr("Error"),
      JOptionPane.ERROR_MESSAGE
    );
  }
}
