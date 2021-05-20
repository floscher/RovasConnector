package app.rovas.josm.action;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.RovasDialog;
import app.rovas.josm.TimeTrackingManager;

public class StopManualTimerAction extends JosmAction {

  private final RovasDialog dialog;

  public StopManualTimerAction(final RovasDialog dialog) {
    super(I18n.tr("Stop manual timer"), "audio-pause", null, null, false);
    this.dialog = dialog;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    TimeTrackingManager.getInstance().stopManualTracker();
    dialog.updateStatusLabel();
    dialog.updateStartStopAction();
  }
}
