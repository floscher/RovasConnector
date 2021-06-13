package app.rovas.josm.gui;

import java.util.Optional;
import javax.swing.Box;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Utils;

import app.rovas.josm.model.StaticConfig;
import app.rovas.josm.util.GuiComponentFactory;
import app.rovas.josm.model.RovasProperties;
import app.rovas.josm.util.UrlProvider;

public final class RovasPreferencePanel extends ApiCredentialsPanel {
  private final JEditorPane feeNote = GuiComponentFactory.createHyperlinkedMultilineLabel(
    "<html>" +
      I18n.tr(
        // i18n: {0} will be replaced by a decimal number with 2 decimal places
        // i18n: {1} will be replaced by a link labeled with "Rovas connector"
        "To reward the authors of this {1}, a fee equal to {0}% of the amount you earn from reports created by the plugin will be levied on those earnings.",
        String.format("%.2f", StaticConfig.ASSET_USAGE_FEE * 100),
        UrlProvider.toHtmlHyperlink(
          UrlProvider.getInstance().node(StaticConfig.ROVAS_CONNECTOR_PROJECT_ID),
          I18n.tr("Rovas connector plugin").replace(" ", "&nbsp;")
        )
      ) +
    "</html>"
  );

  private final JLabel inactivityToleranceLabel = new JLabel(I18n.tr("Timer tolerance"));
  private final SpinnerNumberModel inactivityToleranceModel = new SpinnerNumberModel(
    Utils.clamp(
      RovasProperties.INACTIVITY_TOLERANCE.get(),
      RovasProperties.INACTIVITY_TOLERANCE_MIN_VALUE,
      RovasProperties.INACTIVITY_TOLERANCE_MAX_VALUE
    ),
    RovasProperties.INACTIVITY_TOLERANCE_MIN_VALUE,
    RovasProperties.INACTIVITY_TOLERANCE_MAX_VALUE,
    1
  );
  private final JSpinner inactivityToleranceValue = GuiComponentFactory.createSpinner(inactivityToleranceModel, 5, true);
  private final JLabel inactivityToleranceDescription = GuiComponentFactory.createLabel(I18n.tr("the lag in seconds after last edit to be counted as active time"), false);

  public RovasPreferencePanel() {
    super(false);
    extendGui();

    ExpertToggleAction.addVisibilitySwitcher(inactivityToleranceLabel);
    ExpertToggleAction.addVisibilitySwitcher(inactivityToleranceValue);
    ExpertToggleAction.addVisibilitySwitcher(inactivityToleranceDescription);
  }

  /**
   * Extends the GUI that was previously added by {@link ApiCredentialsPanel#buildGui()}, by adding more components.
   */
  @SuppressWarnings("JavadocReference")
  private void extendGui() {
    add(new JPanel(), GBC_COLUMN_A);
    add(GuiComponentFactory.createWrapperPanel(feeNote), GBC_COLUMNS_BCD);

    add(inactivityToleranceLabel, GBC_COLUMN_A);
    add(inactivityToleranceValue, GBC_COLUMN_B);
    add(inactivityToleranceDescription, GBC_COLUMNS_CD);

    add(Box.createVerticalGlue(), GBC_COLUMN_A.fill(GBC.VERTICAL));
  }


  public int getInactivityTolerance() {
    return inactivityToleranceModel.getNumber().intValue();
  }

  public void setInactivityTolerance(final Integer value) {
    inactivityToleranceModel.setValue(
      Utils.clamp(
        Optional.ofNullable(value).orElse(RovasProperties.INACTIVITY_TOLERANCE_DEFAULT_VALUE),
        RovasProperties.INACTIVITY_TOLERANCE_MIN_VALUE,
        RovasProperties.INACTIVITY_TOLERANCE_MAX_VALUE)
    );
  }
}
