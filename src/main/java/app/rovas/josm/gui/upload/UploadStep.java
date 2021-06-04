package app.rovas.josm.gui.upload;

import java.awt.Window;

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
}
