// License: GPL. For details, see LICENSE file.
package app.rovas.josm.gui;

import java.util.Optional;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Utils;

import app.rovas.josm.model.RovasProperties;
import app.rovas.josm.model.StaticConfig;
import app.rovas.josm.util.GuiComponentFactory;
import app.rovas.josm.util.UrlProvider;

/**
 * The panel in the JOSM settings that contains the main settings a user can modify about the plugin
 */
public final class RovasPreferencePanel extends ApiCredentialsPanel {
  private final JEditorPane feeNote = GuiComponentFactory.createHyperlinkedMultilineLabel(
    "<html>" +
      I18n.tr(
        // i18n: {0} will be replaced by a decimal number with 2 decimal places
        // i18n: {1} will be replaced by a link labeled with "Rovas connector"
        "To reward the authors of this {1}, a fee equal to {0}% of the amount you earn from reports created by the plugin will be levied on those earnings.",
        String.format("%.2f", StaticConfig.ASSET_USAGE_FEE * 100),
        UrlProvider.toHtmlHyperlink(
          UrlProvider.getInstance().node(
            RovasProperties.DEVELOPER.get()
              ? StaticConfig.ROVAS_CONNECTOR_PROJECT_ID_DEV
              : StaticConfig.ROVAS_CONNECTOR_PROJECT_ID
          ),
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
  private final JTextComponent inactivityToleranceDescription = new JMultilineLabel(I18n.tr("the lag in seconds after last edit to be counted as active time"), false);

  private final JCheckBox unpaidEditorCheckbox = new JCheckBox("<html>" + I18n.tr("I am not paid for JOSM work by a company (you will only be prompted to send a work report, if you confirm this)") + "</html>");

  /**
   * Creates a new panel for modifying the preferences
   */
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

    add(new JPanel(), GBC_COLUMN_A);
    add(unpaidEditorCheckbox, GBC_COLUMNS_BCD);

    add(Box.createVerticalGlue(), GBC_COLUMN_A.fill(GBC.VERTICAL));
  }

  /**
   * @return the inactivity tolerance that is currently set in the associated number spinner
   */
  public int getInactivityTolerance() {
    return inactivityToleranceModel.getNumber().intValue();
  }

  /**
   * Sets the displayed inactivity tolerance
   * @param value the new inactivity tolerance to set
   */
  public void setInactivityTolerance(final Integer value) {
    inactivityToleranceModel.setValue(
      Utils.clamp(
        Optional.ofNullable(value).orElse(RovasProperties.INACTIVITY_TOLERANCE_DEFAULT_VALUE),
        RovasProperties.INACTIVITY_TOLERANCE_MIN_VALUE,
        RovasProperties.INACTIVITY_TOLERANCE_MAX_VALUE)
    );
  }

  /**
   * @return {@code true} iff the unpaid editor checkbox is checked
   */
  public boolean isUnpaidEditor() {
    return unpaidEditorCheckbox.isSelected();
  }

  /**
   * Sets the state of the unpaid editor checkbox
   * @param isUnpaidEditor the new checked state, {@code true} means checked, otherwise it will be unchecked
   */
  public void setUnpaidEditor(final boolean isUnpaidEditor) {
    unpaidEditorCheckbox.setSelected(isUnpaidEditor);
  }
}
