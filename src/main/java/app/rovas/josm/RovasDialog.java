package app.rovas.josm;

import java.awt.GridBagLayout;
import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.preferences.AbstractProperty;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.util.GBCUtil;
import app.rovas.josm.util.GuiComponentFactory;

public class RovasDialog extends ToggleDialog implements TimeTrackingUpdateListener {
  private final AbstractProperty.ValueChangeListener<Object> apiConfigurationChangeListener = __ -> updateConfigurationStateLabel();
  private JMultilineLabel configurationStateLabel = new JMultilineLabel("");
  private JLabel counterLabel = new JLabel(I18n.tr("Elapsed time"));
  private JLabel counterValue = GuiComponentFactory.createLabel();
  private JLabel statusLabel = new JLabel(I18n.tr("Status"));
  private JLabel statusValue = GuiComponentFactory.createLabel(I18n.tr("automatic, all changes (+" + RovasProperties.INACTIVITY_TOLERANCE_SECONDS.get() + "s)"), false);

  private GBC GBC_FULL_WIDTH = GBCUtil.fixedToColumn(0, GBC.eol().insets(5).span(2).fill(GBC.HORIZONTAL));
  private GBC GBC_LEFT_COLUMN = GBCUtil.fixedToColumn(0, GBC.std().insets(5).span(1));
  private GBC GBC_RIGHT_COLUMN = GBCUtil.fixedToColumn(1, GBC.eol().insets(5).span(1).fill(GBC.HORIZONTAL));

  public RovasDialog() {
    super(I18n.tr("Rovas"), "rovas_logo", I18n.tr("Time tracking with Rovas"), null, 150, true, RovasPreference.class, false);

    JPanel panel = new JPanel(new GridBagLayout());

    panel.add(configurationStateLabel, GBC_FULL_WIDTH);
    panel.add(counterLabel, GBC_LEFT_COLUMN);
    panel.add(counterValue, GBC_RIGHT_COLUMN);
    panel.add(statusLabel, GBC_LEFT_COLUMN);
    panel.add(statusValue, GBC_RIGHT_COLUMN);
    panel.add(Box.createVerticalGlue(), GBC_LEFT_COLUMN.fill(GBC.VERTICAL));

    createLayout(panel, false, new ArrayList<>());

    TimeTrackingManager.getInstance().addAndFireTimeTrackingUpdateListener(this);
    RovasProperties.ROVAS_ACTIVE_PROJECT_ID.addListener(apiConfigurationChangeListener);
    RovasProperties.ROVAS_API_KEY.addListener(apiConfigurationChangeListener);
    RovasProperties.ROVAS_API_TOKEN.addListener(apiConfigurationChangeListener);
    updateConfigurationStateLabel();
  }

  public void updateConfigurationStateLabel() {
    configurationStateLabel.setText(
      "<html><span style='color:#999999'>" + (
        RovasProperties.getActiveProjectID() != null && RovasProperties.getApiKey() != null && RovasProperties.getApiToken() != null
          ? I18n.tr("The Rovas connection settings are configured.")
          : I18n.tr("The plugin is not configured. Click the preferences icon at the top right of this dialog.")
      ) + "</span></html>"
    );
  }

  @Override
  public void destroy() {
    super.destroy();
    TimeTrackingManager.getInstance().removeTimeTrackingUpdateListener(this);
  }

  @Override
  public void updateNumberOfTrackedSeconds(final long n) {
    counterValue.setText("<html><style>strong { font-size:1.8em}</style><strong>" + (n / 3600L) + "</strong>h <strong>" + (n % 3600 / 60) + "</strong>m <strong>" + (n % 60) + "</strong>s</html>");
  }
}
