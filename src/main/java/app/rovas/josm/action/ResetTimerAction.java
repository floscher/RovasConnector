// License: GPL. For details, see LICENSE file.
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

  private static final String TRANSLATABLE_LABEL = I18n.marktr("Reset timer");

  /**
   * Creates the default action to reset the timer to an arbitrary value (0 by default).
   */
  public ResetTimerAction() {
    super(
      I18n.tr(TRANSLATABLE_LABEL),
      "preferences/reset",
      I18n.tr("Reset the timer (either to 0 or an arbitrary value)"),
      null,
      false
    );
  }

  /**
   * Open the reset dialog
   * @param e parameter is ignored (see {@link JosmAction#actionPerformed(ActionEvent)})
   */
  @Override
  public void actionPerformed(final ActionEvent e) {
    final ResetPanel resetPanel = new ResetPanel();
    if (
      JOptionPane.showConfirmDialog(
        MainApplication.getMainFrame(),
        resetPanel,
        I18n.tr(TRANSLATABLE_LABEL),
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE
      ) == JOptionPane.OK_OPTION
    ) {
      TimeTrackingManager.getInstance().setCurrentlyTrackedSeconds(resetPanel.getMinutes() * 60L);
    }
  }

  /**
   * The panel for the reset dialog
   */
  private final static class ResetPanel extends JPanel {
    private final SpinnerNumberModel hourModel = new SpinnerNumberModel(0, 0, TimeConverterUtil.MAX_HOURS, 1);
    private final SpinnerNumberModel minuteModel = new SpinnerNumberModel(0, 0, 59, 1);

    private ResetPanel() {
      super(new FlowLayout(FlowLayout.CENTER));
      add(GuiComponentFactory.createLabel(I18n.tr("Reset the timer to"), false));
      add(GuiComponentFactory.createSpinner(hourModel, 3, true));
      add(GuiComponentFactory.createLabel(I18nStrings.trShorthandForHours(), false));
      add(GuiComponentFactory.createSpinner(minuteModel, 3, true));
      add(GuiComponentFactory.createLabel(I18nStrings.trShorthandForMinutes(), false));
    }

    private int getMinutes() {
      return hourModel.getNumber().intValue() * 60 + minuteModel.getNumber().intValue();
    }
  }
}
