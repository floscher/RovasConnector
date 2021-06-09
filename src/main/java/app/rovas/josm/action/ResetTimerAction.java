package app.rovas.josm.action;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.model.TimeTrackingManager;
import app.rovas.josm.util.GuiComponentFactory;
import app.rovas.josm.util.I18nStrings;
import app.rovas.josm.util.TimeConverterUtil;

/**
 * Resets the timer to 0 or an arbitrary value
 */
public class ResetTimerAction extends JosmAction {

  private static final String translatableLabel = I18n.marktr("Reset timer");

  public ResetTimerAction() {
    super(
      I18n.tr(translatableLabel),
      "preferences/reset",
      I18n.tr("Reset the timer (either to 0 or an arbitrary value)"),
      null,
      false
    );
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    final ResetPanel resetPanel = new ResetPanel();
    if (
      JOptionPane.showConfirmDialog(
        MainApplication.getMainFrame(),
        resetPanel,
        I18n.tr(translatableLabel),
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE
      ) == JOptionPane.OK_OPTION
    ) {
      TimeTrackingManager.getInstance().setCurrentlyTrackedSeconds(resetPanel.getMinutes() * 60);
    }
  }

  private static class ResetPanel extends JPanel {
    private final SpinnerNumberModel hourModel = new SpinnerNumberModel(0, 0, TimeConverterUtil.MAX_HOURS, 1);
    private final SpinnerNumberModel minuteModel = new SpinnerNumberModel(0, 0, 59, 1);

    public ResetPanel() {
      setLayout(new FlowLayout(FlowLayout.CENTER));
      add(GuiComponentFactory.createLabel(I18n.tr("Reset the timer to"), false));
      add(GuiComponentFactory.createSpinner(hourModel, 3, true));
      add(GuiComponentFactory.createLabel(I18nStrings.trShorthandForHours(), false));
      add(GuiComponentFactory.createSpinner(minuteModel, 3, true));
      add(GuiComponentFactory.createLabel(I18nStrings.trShorthandForMinutes(), false));
    }

    public int getMinutes() {
      return hourModel.getNumber().intValue() * 60 + minuteModel.getNumber().intValue();
    }
  }
}
