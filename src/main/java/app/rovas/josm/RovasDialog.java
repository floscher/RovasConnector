package app.rovas.josm;

import java.awt.Font;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.tools.I18n;

public class RovasDialog extends ToggleDialog implements TimeTrackingUpdateListener {
  private JLabel counterLabel = new JLabel();


  public RovasDialog() {
    super(I18n.tr("Rovas"), "rovas_logo", I18n.tr("Time tracking with Rovas"), null, 150, true, RovasPreference.class, false);

    JPanel panel = new JPanel();
    counterLabel.setFont(counterLabel.getFont().deriveFont(Font.PLAIN));
    panel.add(counterLabel);

    createLayout(panel, false, new ArrayList<>());

    TimeTrackingManager.getInstance().addAndFireTimeTrackingUpdateListener(this);
  }

  @Override
  public void destroy() {
    super.destroy();
    TimeTrackingManager.getInstance().removeTimeTrackingUpdateListener(this);
  }

  @Override
  public void updateNumberOfTrackedChanges(long n) {
    counterLabel.setText(n + " changes");
  }
}
