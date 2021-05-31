package app.rovas.josm.gui.upload;

import java.awt.Window;

public abstract class UploadStep {
  protected final Window parent;

  public UploadStep(final Window parent) {
    this.parent = parent;
  }

  public abstract void showStep();
}
