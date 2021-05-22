package app.rovas.josm;

import java.awt.GridBagLayout;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import javax.swing.Box;
import javax.swing.JLabel;

import org.openstreetmap.josm.data.preferences.AbstractProperty;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.action.ResetTimerAction;
import app.rovas.josm.util.GBCUtil;
import app.rovas.josm.util.I18nStrings;

public class RovasDialog extends ToggleDialog implements TimeTrackingUpdateListener {
  private final AbstractProperty.ValueChangeListener<Object> apiConfigurationChangeListener = __ -> {
    updateMissingConfigurationWarning();
  };


  private final JMultilineLabel savingStatusValue = new JMultilineLabel("");
  private final JLabel counterLabel = new JLabel(I18n.tr("Active time"));
  private final JLabel counterValue = new JLabel("");
  private final DateTimeFormatter lastDetectedChangeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
  private final JLabel savingStatusLabel = new JLabel(I18n.tr("Saving status"));

  private final SideButton resetButton = new SideButton(new ResetTimerAction());

  private final GBC GBC_LEFT_COLUMN = GBCUtil.fixedToColumn(0, GBC.std().insets(5).span(1)).anchor(GBC.LINE_END);
  private final GBC GBC_RIGHT_COLUMN = GBCUtil.fixedToColumn(1, GBC.eol().insets(5).span(1).fill(GBC.HORIZONTAL));

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

    VerticallyScrollablePanel panel = new VerticallyScrollablePanel(new GridBagLayout());

    panel.add(counterLabel, GBC_LEFT_COLUMN);
    panel.add(counterValue, GBC_RIGHT_COLUMN);
    panel.add(savingStatusLabel, GBC_LEFT_COLUMN);
    panel.add(savingStatusValue, GBC_RIGHT_COLUMN);
    panel.add(Box.createVerticalGlue(), GBC_LEFT_COLUMN.fill(GBC.VERTICAL));

    createLayout(panel.getVerticalScrollPane(), false, Collections.singletonList(resetButton));

    TimeTrackingManager.getInstance().addAndFireTimeTrackingUpdateListener(this);
    RovasProperties.ACTIVE_PROJECT_ID.addListener(apiConfigurationChangeListener);
    RovasProperties.ROVAS_API_KEY.addListener(apiConfigurationChangeListener);
    RovasProperties.ROVAS_API_TOKEN.addListener(apiConfigurationChangeListener);
    RovasProperties.ALWAYS_CREATE_REPORT.addListener(apiConfigurationChangeListener);
    apiConfigurationChangeListener.valueChanged(null);
  }

  public void updateMissingConfigurationWarning() {
    GuiHelper.runInEDT(() ->
      savingStatusValue.setText(
        !RovasProperties.isActiveProjectIdSet() ||
        RovasProperties.ROVAS_API_KEY.get() == null ||
        RovasProperties.ROVAS_API_TOKEN.get() == null
          ? "<html><div style='background:#fdc14b;padding:5px 10px'>" +
            I18n.tr("Complete the plugin setup process and choose if you want to have a report created.") +
            "</div></html>"
          : (
            RovasProperties.ALWAYS_CREATE_REPORT.get()
              ? I18n.tr("a report will be created automatically")
              : I18n.tr("save report manually when uploading")
          )
      )
    );
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
    GuiHelper.runInEDT(() ->
      counterValue.setText(String.format(
        "<html><strong style='font-size:1.8em'>%d</strong>&thinsp;" +
          I18nStrings.trShorthandForHours() +
          "&nbsp;<strong style='font-size:1.8em'>%02d</strong>&thinsp;" +
          I18nStrings.trShorthandForMinutes() +
          "&nbsp;<span style='color:#bbbbbb'>(%d&nbsp;" +
          I18n.tr("seconds") +
          ")</span></html>",
        // 30 seconds are added, so 30 or more seconds within a minute are rounded up to the next full minute
        (n + 30) / 3600L,
        (n + 30) % 3600 / 60,
        n
      ))
    );
  }
}
