package app.rovas.josm.gui;

import java.awt.GridBagLayout;
import java.util.Collections;
import javax.swing.Box;
import javax.swing.JLabel;

import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.action.ResetTimerAction;
import app.rovas.josm.model.RovasPreference;
import app.rovas.josm.model.TimeTrackingManager;
import app.rovas.josm.util.GBCUtil;
import app.rovas.josm.util.I18nStrings;
import app.rovas.josm.util.TimeConverterUtil;

/**
 * The main dialog that displays a live timer of the time that the plugin has tracked so far.
 */
public class RovasDialog extends ToggleDialog implements TimeTrackingUpdateListener {
  private static final GBC GBC_LEFT_COLUMN = GBCUtil.fixedToColumn(0, GBC.std().insets(5).span(1)).anchor(GBC.LINE_END);
  private static final GBC GBC_RIGHT_COLUMN = GBCUtil.fixedToColumn(1, GBC.eol().insets(5).span(1).fill(GBC.HORIZONTAL));

  private final JLabel counterLabel = new JLabel(I18n.tr("Active time"));
  private final JLabel counterValue = new JLabel("");

  private final SideButton resetButton = new SideButton(new ResetTimerAction());

  /**
   * Creates a new dialog and registers it with the {@link TimeTrackingManager}
   */
  public RovasDialog() {
    super(
      I18n.tr("Rovas"),
      "rovas_logo",
      I18n.tr("Time tracking with Rovas"),
      null,
      150,
      true,
      RovasPreference.class,
      false
    );

    final VerticallyScrollablePanel panel = new VerticallyScrollablePanel(new GridBagLayout());

    panel.add(counterLabel, GBC_LEFT_COLUMN);
    panel.add(counterValue, GBC_RIGHT_COLUMN);

    panel.add(Box.createVerticalGlue(), GBC_LEFT_COLUMN.fill(GBC.VERTICAL));

    createLayout(panel.getVerticalScrollPane(), false, Collections.singletonList(resetButton));

    TimeTrackingManager.getInstance().addAndFireTimeTrackingUpdateListener(this);
  }

  @Override
  public void destroy() {
    super.destroy();
    TimeTrackingManager.getInstance().removeTimeTrackingUpdateListener(this);
  }

  @Override
  public void updateNumberOfTrackedSeconds(final long n) {
    if (n < 0) {
      throw new IllegalArgumentException("Number of tracked seconds must always be non-negative (was" + n + ")!");
    }
    final long minutes = TimeConverterUtil.secondsToMinutes(n);
    GuiHelper.runInEDT(() ->
      counterValue.setText(String.format(
        "<html><strong style='font-size:1.8em'>%d</strong>&#8239;" +
          I18nStrings.trShorthandForHours() +
          "&nbsp;<strong style='font-size:1.8em'>%02d</strong>&#8239;" +
          I18nStrings.trShorthandForMinutes() +
          "&nbsp;<span style='color:#bbbbbb'>(%d&nbsp;" +
          I18n.tr("seconds") +
          ")</span></html>",
        minutes / 60,
        minutes % 60,
        n
      ))
    );
  }
}
