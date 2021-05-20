package app.rovas.josm.action;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.RovasDialog;
import app.rovas.josm.TimeTrackingManager;

public class StartManualTimerAction extends JosmAction {

  private final RovasDialog dialog;

  public StartManualTimerAction(final RovasDialog dialog) {
    super(I18n.tr("Start manual timer"), "audio-play", null, null, false);
    this.dialog = dialog;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    TimeTrackingManager.getInstance().startManualTracker();
    dialog.updateStatusLabel();
    dialog.updateStartStopAction();
  }
}
