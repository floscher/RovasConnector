package app.rovas.josm;

import java.awt.GridBagLayout;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.preferences.AbstractProperty;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

import app.rovas.josm.action.ResetManualTimerAction;
import app.rovas.josm.action.StartManualTimerAction;
import app.rovas.josm.action.StopManualTimerAction;
import app.rovas.josm.util.GBCUtil;
import app.rovas.josm.util.GuiComponentFactory;

public class RovasDialog extends ToggleDialog implements TimeTrackingUpdateListener {
  private final ImageIcon ICON_SCANNER_ANIMATION =
    new ImageIcon(Objects.requireNonNull(RovasDialog.class.getResource("/images/load.gif")));

  private final AbstractProperty.ValueChangeListener<Object> apiConfigurationChangeListener = __ -> {
    updateMissingConfigurationWarning();
    updateStatusLabel();
  };


  private final JMultilineLabel savingStatusValue = new JMultilineLabel("");
  private final JLabel counterLabel = new JLabel(I18n.tr("Active time"));
  private final JLabel counterValue = new JLabel("");
  private final JLabel statusLabel = new JLabel(I18n.tr("Status"));
  private final JLabel statusValue = GuiComponentFactory.createLabel("", false);
  private final JLabel statusDetails = GuiComponentFactory.createLabel("", false);
  private final DateTimeFormatter lastDetectedChangeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
  private final JLabel savingStatusLabel = new JLabel(I18n.tr("Saving status"));

  private final Action startManualTimerAction = new StartManualTimerAction(this);
  private final Action stopManualTimerAction = new StopManualTimerAction(this);
  private final SideButton playPauseButton = new SideButton(startManualTimerAction);

  private final SideButton resetButton = new SideButton(new ResetManualTimerAction());

  private final GBC GBC_FULL_WIDTH = GBCUtil.fixedToColumn(0, GBC.eol().insets(0).span(2).fill(GBC.HORIZONTAL));
  private final GBC GBC_LEFT_COLUMN = GBCUtil.fixedToColumn(0, GBC.std().insets(5).span(1)).anchor(GBC.LINE_END);
  private final GBC GBC_RIGHT_COLUMN = GBCUtil.fixedToColumn(1, GBC.eol().insets(5).span(1).fill(GBC.HORIZONTAL));

  public RovasDialog() {
    super(I18n.tr("Rovas"), "rovas_logo", I18n.tr("Time tracking with Rovas"), null, 150, true, RovasPreference.class, false);

    JPanel panel = new JPanel(new GridBagLayout());

    panel.add(counterLabel, GBC_LEFT_COLUMN);
    panel.add(counterValue, GBC_RIGHT_COLUMN);
    panel.add(statusLabel, GBC_LEFT_COLUMN);
    panel.add(statusValue, GBC_RIGHT_COLUMN);
    panel.add(GBC.glue(1, 1), GBC_LEFT_COLUMN);
    panel.add(statusDetails, GBC_RIGHT_COLUMN);
    panel.add(savingStatusLabel, GBC_LEFT_COLUMN);
    panel.add(savingStatusValue, GBC_RIGHT_COLUMN);
    panel.add(Box.createVerticalGlue(), GBC_LEFT_COLUMN.fill(GBC.VERTICAL));

    createLayout(panel, false, Arrays.asList(playPauseButton, resetButton));

    TimeTrackingManager.getInstance().addAndFireTimeTrackingUpdateListener(this);
    RovasProperties.ACTIVE_PROJECT_ID.addListener(apiConfigurationChangeListener);
    RovasProperties.ROVAS_API_KEY.addListener(apiConfigurationChangeListener);
    RovasProperties.ROVAS_API_TOKEN.addListener(apiConfigurationChangeListener);
    RovasProperties.ALWAYS_CREATE_REPORT.addListener(apiConfigurationChangeListener);
    RovasProperties.INACTIVITY_TOLERANCE.addListener(apiConfigurationChangeListener);

    updateStartStopAction();
    updateMissingConfigurationWarning();
    updateStatusLabel();
  }

  public void updateMissingConfigurationWarning() {
    savingStatusValue.setText(
      !RovasProperties.isActiveProjectIdSet() ||
      RovasProperties.ROVAS_API_KEY.get() == null ||
      RovasProperties.ROVAS_API_TOKEN.get() == null
        ? "<html><div style='background:#fdc14b;padding:5px 10px'>" +
          I18n.tr("The plugin is not configured to send work reports to Rovas. Click the preferences icon at the top right of this dialog.") +
          "</div></html>"
        : (
          RovasProperties.ALWAYS_CREATE_REPORT.get()
            ? I18n.tr("A report is automatically created")
            : I18n.tr("A report is only created, if requested manually")
        )
    );
  }

  public void updateStatusLabel() {
    if (TimeTrackingManager.getInstance().isManual()) {
      statusValue.setIcon(ImageProvider.get("audio-play"));
      statusValue.setText(I18n.tr("manual timer running until stopped again"));
    } else {
      statusValue.setIcon(ICON_SCANNER_ANIMATION);
      statusValue.setText(I18n.tr("automatic change detection (+{0}\u2009s)", RovasProperties.INACTIVITY_TOLERANCE.get()));
    }
  }

  @Override
  public void destroy() {
    super.destroy();
    TimeTrackingManager.getInstance().stopManualTracker();
    TimeTrackingManager.getInstance().removeTimeTrackingUpdateListener(this);
  }

  @Override
  public void updateNumberOfTrackedSeconds(final long n) {
    if (n < 0) {
      throw new IllegalArgumentException("Number of tracked seconds must always be non-negative (was" + n + ")!");
    }
    SwingUtilities.invokeLater(() -> {
      counterValue.setText(String.format(
        "<html><strong style='font-size:1.8em'>%d</strong>&thinsp;" +
          I18n.trc("shorthand for hours", "h") +
          "&nbsp;<strong style='font-size:1.8em'>%02d</strong>&thinsp;" +
          I18n.trc("shorthand for minutes", "m") +
          "&nbsp;<span style='color:#bbbbbb'>(%d&nbsp;" +
          I18n.tr("seconds") +
          ")</span></html>",
        // 30 seconds are added, so 30 or more seconds within a minute are rounded up to the next full minute
        (n + 30) / 3600L,
        (n + 30) % 3600 / 60,
        n
      ));
      final Long manualSeconds = TimeTrackingManager.getInstance().getManualTimerDurationSeconds();
      if (manualSeconds != null) {
        statusDetails.setText(String.format("%d:%02d:%02d", manualSeconds / 3600, manualSeconds % 3600 / 60, manualSeconds % 60));
      } else {
        final Long lastDetectedChangeTimestamp = TimeTrackingManager.getInstance().getLastDetectedChangeTimestamp();
        if (lastDetectedChangeTimestamp != null) {
          statusDetails.setText(I18n.tr("latest change detected at") + " " + lastDetectedChangeFormatter.format(Instant.ofEpochSecond(lastDetectedChangeTimestamp)));
        } else {
          statusDetails.setText("");
        }
      }
    });
  }

  public void updateStartStopAction() {
    playPauseButton.setAction(
      TimeTrackingManager.getInstance().isManual()
        ? stopManualTimerAction
        : startManualTimerAction
    );
  }
}
