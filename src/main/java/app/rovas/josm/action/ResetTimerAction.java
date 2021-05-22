package app.rovas.josm.action;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.TimeTrackingManager;

/**
 * Resets the timer to 0 or an arbitrary value
 */
public class ResetTimerAction extends JosmAction {

  public ResetTimerAction() {
    super(
      I18n.tr("Reset timer"),
      "preferences/reset",
      I18n.tr("Reset the timer (either to 0 or an arbitrary value)"),
      null,
      false
    );
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO: Allow for resetting to an arbitrary time, not just to 0:00.
    if (
      JOptionPane.showConfirmDialog(
        MainApplication.getMainFrame(),
        I18n.tr("Do you really want to reset the recorded work time to 0:00 hours?"),
        I18n.tr("Reset timer"),
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE
      ) == JOptionPane.YES_OPTION
    ) {
      TimeTrackingManager.getInstance().setCurrentlyTrackedSeconds(0);
    }
  }
}
