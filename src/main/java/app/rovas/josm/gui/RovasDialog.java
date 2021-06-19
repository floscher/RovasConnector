// License: GPL. For details, see LICENSE file.
package app.rovas.josm.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Collections;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

import app.rovas.josm.RovasPlugin;
import app.rovas.josm.action.ResetTimerAction;
import app.rovas.josm.model.RovasPreference;
import app.rovas.josm.model.RovasProperties;
import app.rovas.josm.model.TimeTrackingManager;
import app.rovas.josm.util.GBCUtil;
import app.rovas.josm.util.GuiComponentFactory;
import app.rovas.josm.util.I18nStrings;
import app.rovas.josm.util.TimeConverterUtil;

/**
 * The main dialog that displays a live timer of the time that the plugin has tracked so far.
 */
public class RovasDialog extends ToggleDialog implements TimeTrackingUpdateListener {
  private static final GBC GBC_LEFT_COLUMN = GBCUtil.fixedToColumn(0, GBC.std().insets(5).span(1)).anchor(GBC.LINE_END);
  private static final GBC GBC_RIGHT_COLUMN = GBCUtil.fixedToColumn(1, GBC.eol().insets(5).span(1).fill(GBC.HORIZONTAL));
  private static final GBC GBC_BOTH_COLUMNS = GBCUtil.fixedToColumn(0, GBC.eol().insets(5).span(2).fill(GBC.HORIZONTAL));

  private final JLabel timerValue = new JLabel(); // populated later with appropriate text

  private final JMultilineLabel previousTimeLabel = new JMultilineLabel(""); // populated later with appropriate text
  private final JPanel previousTimePanel = new JPanel(new BorderLayout());

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

    panel.add(new JLabel(I18n.tr("Active time")), GBC_LEFT_COLUMN);
    panel.add(timerValue, GBC_RIGHT_COLUMN);

    updatePreviousTime();
    previousTimePanel.add(previousTimeLabel, BorderLayout.CENTER);
    previousTimePanel.add(
      GuiComponentFactory.createWrapperPanel(
        new FlowLayout(),
        new JButton(new HandlePreviouslyTrackedTimeAction(true, I18n.tr("Add"))),
        new JButton(new HandlePreviouslyTrackedTimeAction(false, I18n.tr("Discard")))
      ),
      BorderLayout.SOUTH
    );

    panel.add(previousTimePanel, GBC_BOTH_COLUMNS);

    panel.add(Box.createVerticalGlue(), GBC_LEFT_COLUMN.fill(GBC.VERTICAL));

    createLayout(
      panel.getVerticalScrollPane(),
      false,
      Collections.singletonList(
        new SideButton(new ResetTimerAction())
      )
    );

    TimeTrackingManager.getInstance().addAndFireTimeTrackingUpdateListener(this);
  }

  private void updatePreviousTime() {
    final int previousMinutes = TimeConverterUtil.secondsToMinutes(TimeTrackingManager.getInstance().getPreviouslyTrackedSeconds());
    final String commonMessage = I18n.trn(
      "Previously you already tracked {0} minute for Rovas without creating a work report.",
      "Previously you already tracked {0} minutes for Rovas without creating a work report.",
      previousMinutes,
      previousMinutes
    );

    if (previousMinutes > 0) {
      new Notification(commonMessage + "<br>" + I18n.tr("If you want to add that time to your current timer, you can do that in the Rovas dialog."))
        .setDuration(Notification.TIME_LONG)
        .setIcon(RovasPlugin.LOGO.setSize(ImageProvider.ImageSizes.DEFAULT).get())
        .show();
    }
    previousTimePanel.setVisible(previousMinutes > 0);
    previousTimeLabel.setText(commonMessage + "<br>" + I18n.tr("Should this time be added to the timer, or discarded?"));
  }

  @Override
  public void destroy() {
    super.destroy();
    TimeTrackingManager.getInstance().removeTimeTrackingUpdateListener(this);
    RovasProperties.ALREADY_TRACKED_TIME.put(TimeTrackingManager.getInstance().commit());
  }

  @Override
  public void updateNumberOfTrackedSeconds(final long n) {
    if (n < 0) {
      throw new IllegalArgumentException("Number of tracked seconds must always be non-negative (was" + n + ")!");
    }
    final long minutes = TimeConverterUtil.secondsToMinutes(n);
    GuiHelper.runInEDT(() ->
      timerValue.setText(String.format(
        "<html><strong style='font-size:1.8em'>%d</strong>&#8239;" +
          I18nStrings.trShorthandForHours() +
          "&nbsp;<strong style='font-size:1.8em'>%02d</strong>&#8239;" +
          I18nStrings.trShorthandForMinutes() +
          "</html>",
        minutes / 60,
        minutes % 60
      ))
    );
  }

  /**
   * Action for either adding or discarding time that has previously been tracked, but for which
   * no work report has been submitted yet.
   */
  private final class HandlePreviouslyTrackedTimeAction extends AbstractAction {
    private final boolean shouldBeAdded;

    private HandlePreviouslyTrackedTimeAction(final boolean shouldBeAdded, final String name) {
      super(name);
      this.shouldBeAdded = shouldBeAdded;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      TimeTrackingManager.getInstance().handlePreviouslyTrackedSeconds(shouldBeAdded);
      previousTimePanel.setVisible(false);
    }
  }
}
