package app.rovas.josm.action;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.I18n;

public class StartManualTimerAction extends JosmAction {

  public StartManualTimerAction() {
    super(I18n.tr("Start manual timer"), "audio-play", null, null, false);
    setEnabled(false);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO: Implement
  }
}
