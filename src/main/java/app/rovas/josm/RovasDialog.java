package app.rovas.josm;

import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.preferences.AbstractProperty;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.action.ResetManualTimerAction;
import app.rovas.josm.action.StartManualTimerAction;
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
  private final JLabel counterLabel = new JLabel(I18n.tr("Elapsed time"));
  private final JLabel counterValue = GuiComponentFactory.createLabel();
  private final JLabel statusLabel = new JLabel(I18n.tr("Status"));
  private final JLabel statusValue = GuiComponentFactory.createLabel("", false);
  private final JLabel savingStatusLabel = new JLabel(I18n.tr("Saving status"));
  private final SideButton playPauseButton = new SideButton(new StartManualTimerAction());
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
    panel.add(savingStatusLabel, GBC_LEFT_COLUMN);
    panel.add(savingStatusValue, GBC_RIGHT_COLUMN);
    panel.add(Box.createVerticalGlue(), GBC_LEFT_COLUMN.fill(GBC.VERTICAL));

    createLayout(panel, false, Arrays.asList(playPauseButton, resetButton));

    TimeTrackingManager.getInstance().addAndFireTimeTrackingUpdateListener(this);
    RovasProperties.ROVAS_ACTIVE_PROJECT_ID.addListener(apiConfigurationChangeListener);
    RovasProperties.ROVAS_API_KEY.addListener(apiConfigurationChangeListener);
    RovasProperties.ROVAS_API_TOKEN.addListener(apiConfigurationChangeListener);
    RovasProperties.INACTIVITY_TOLERANCE_SECONDS.addListener(apiConfigurationChangeListener);

    updateMissingConfigurationWarning();
    updateStatusLabel();
  }

  public void updateMissingConfigurationWarning() {
    savingStatusValue.setText(
      RovasProperties.ROVAS_ACTIVE_PROJECT_ID.get() == null ||
      RovasProperties.ROVAS_API_KEY.get() == null ||
      RovasProperties.ROVAS_API_TOKEN.get() == null
        ? (
          RovasProperties.ALWAYS_CREATE_REPORT.get()
            ? I18n.tr("A report is automatically created")
            : I18n.tr("A report is only created, if requested manually"))
        : "<html><div style='background:#fdc14b;padding:5px 10px'>" +
          I18n.tr("The plugin is not configured to send work reports to Rovas. Click the preferences icon at the top right of this dialog.") +
          "</div></html>"
    );
  }

  public void updateStatusLabel() {
    statusValue.setIcon(ICON_SCANNER_ANIMATION);
    statusValue.setText(I18n.tr("automatic change detection (+{0}\u2009s)", RovasProperties.INACTIVITY_TOLERANCE_SECONDS.get()));
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
    counterValue.setText(String.format(
      "<html><style>strong { font-size:1.8em}</style><strong>%02d</strong>" +
        I18n.trc("shorthand for hours", "h") +
        " <strong>%02d</strong>" +
        I18n.trc("shorthand for minutes", "m") +
        " <span style='color:#bbbbbb'>(%d " +
        I18n.tr("seconds") +
        ")</span></html>",
      // 30 seconds are added, so 30 or more seconds within a minute are rounded up to the next full minute
      (n + 30) / 3600L,
      (n + 30) % 3600 / 60,
      n
    ));
  }
}
