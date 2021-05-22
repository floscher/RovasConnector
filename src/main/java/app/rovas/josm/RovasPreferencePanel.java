package app.rovas.josm;

import java.awt.GridBagLayout;
import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.drew.lang.annotations.Nullable;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Utils;

import app.rovas.josm.util.GBCUtil;
import app.rovas.josm.util.GuiComponentFactory;
import app.rovas.josm.util.I18nStrings;
import app.rovas.josm.util.URIs;

public final class RovasPreferencePanel extends VerticallyScrollablePanel {

  private static final GBC GBC_COLUMN_A = GBCUtil.fixedToColumn(0, GBC.std().insets(5).span(1).anchor(GBC.LINE_END));
  private static final GBC GBC_COLUMNS_BC = GBCUtil.fixedToColumn(1, GBC.std().insets(5).span(2).fill(GBC.HORIZONTAL).anchor(GBC.LINE_START));
  private static final GBC GBC_COLUMN_B = GBCUtil.fixedToColumn(1, GBC.std().insets(5).span(1).fill(GBC.HORIZONTAL).weight(0.0, 0.0).anchor(GBC.LINE_START));
  private static final GBC GBC_COLUMN_D = GBCUtil.fixedToColumn(3, GBC.std().insets(5).span(1).fill(GBC.HORIZONTAL).anchor(GBC.LINE_START));
  private static final GBC GBC_COLUMNS_CD = GBCUtil.fixedToColumn(2, GBC.std().insets(5).span(2).fill(GBC.HORIZONTAL).anchor(GBC.LINE_START));
  private static final GBC GBC_COLUMNS_BCD = GBCUtil.fixedToColumn(1, GBC.std().insets(5).span(3).fill(GBC.HORIZONTAL).anchor(GBC.LINE_START));

  private final JLabel apiKeyLabel = new JLabel(I18n.tr("API key"));
  private final JTextField apiKeyField = new JPasswordField(35);

  private final JLabel apiTokenLabel = new JLabel(I18n.tr("API token"));
  private final JTextField apiTokenField = new JPasswordField(35);

  private final JEditorPane seeProfilePageNote = GuiComponentFactory.createHyperlinkedMultilineLabel(
    "<html>" +
    I18n.tr(
      "Values for these fields can be found on your {0}.",
      URIs.toHtmlHyperlink(URIs.userProfile(), I18n.tr("Rovas profile page"))
    ) +
    "</html>"
  );

  private final JLabel activeProjectIdLabel = new JLabel(I18n.tr("Project ID"));
  private final SpinnerNumberModel activeProjectIdSpinnerModel = new SpinnerNumberModel(RovasProperties.ACTIVE_PROJECT_ID_NO_VALUE, RovasProperties.ACTIVE_PROJECT_ID_NO_VALUE, Integer.MAX_VALUE, 0);
  private final JSpinner activeProjectIdSpinner = GuiComponentFactory.createSpinner(activeProjectIdSpinnerModel, 5, true);
  private URI activeProjectURI = null;
  private final JButton activeProjectOpenButton = new JButton(I18n.tr("Open project page in Rovas"));
  private final JLabel activeProjectIdDescription = GuiComponentFactory.createLabel(I18n.tr("(the parent project of the created work reports)"), false);

  private final JLabel alwaysCreateWorkReportLabel = new JLabel(I18n.tr("Work reports"));
  private final JCheckBox alwaysCreateWorkReportValue = new JCheckBox(I18n.tr("Always create a work report by default when uploading OSM data"));

  private final JEditorPane verificationNote = GuiComponentFactory.createHyperlinkedMultilineLabel(
    I18nStrings.trVerificationWarningWithHyperlink()
  );

  private final URI connectorURI = URIs.project(RovasProperties.ROVAS_CONNECTOR_PROJECT_ID);
  private final JEditorPane feeNote = GuiComponentFactory.createHyperlinkedMultilineLabel(
    "<html>" +
      I18n.tr(
        // i18n: {0} will be replaced by a decimal number with 2 decimal places
        // i18n: {1} will be replaced by a link labeled with "Rovas connector"
        "To reward the authors of this {1}, a fee equal to {0}% of the amount you earn from reports created by the plugin will be levied on those earnings.",
        String.format("%.2f", RovasProperties.ASSET_USAGE_FEE * 100),
        String.format("<a href=\"%s\">%s</a>", connectorURI == null ? "#" : connectorURI, I18n.tr("Rovas connector plugin").replace(" ", "&nbsp;"))
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
    super();

    // Update the URL that the button opens
    activeProjectIdSpinnerModel.addChangeListener((changeEvent) -> {
      final int currentActiveProjectId = activeProjectIdSpinnerModel.getNumber().intValue();
      if (currentActiveProjectId >= RovasProperties.ACTIVE_PROJECT_ID_MIN_VALUE) {
        activeProjectURI = URIs.project(currentActiveProjectId);
      } else {
        activeProjectURI = null;
      }
      activeProjectOpenButton.setEnabled(activeProjectURI != null);
    });
    activeProjectOpenButton.setEnabled(false);
    activeProjectOpenButton.addActionListener((e) -> {
      final URI currentActiveProjectURI = activeProjectURI;
      if (currentActiveProjectURI != null) {
        OpenBrowser.displayUrl(currentActiveProjectURI);
      } else {
        activeProjectOpenButton.setEnabled(false);
      }
    });

    buildGui();

    ExpertToggleAction.addVisibilitySwitcher(inactivityToleranceLabel);
    ExpertToggleAction.addVisibilitySwitcher(inactivityToleranceValue);
    ExpertToggleAction.addVisibilitySwitcher(inactivityToleranceDescription);
  }

  /**
   * Adding the individual components and lay them out.
   */
  private void buildGui() {
    setLayout(new GridBagLayout());

    // API key and token
    add(apiKeyLabel, GBC_COLUMN_A);
    add(apiKeyField, GBC_COLUMNS_BC);
    add(Box.createHorizontalGlue(), GBC_COLUMN_D);
    add(apiTokenLabel, GBC_COLUMN_A);
    add(apiTokenField, GBC_COLUMNS_BC);
    add(Box.createHorizontalGlue(), GBC_COLUMN_D);

    add(Box.createHorizontalGlue(), GBC_COLUMN_A);
    add(GuiComponentFactory.createWrapperPanel(seeProfilePageNote), GBC_COLUMNS_BCD);

    add(activeProjectIdLabel, GBC_COLUMN_A);
    add(activeProjectIdSpinner, GBC_COLUMN_B);
    add(activeProjectIdDescription, GBC_COLUMNS_CD);
    add(Box.createHorizontalGlue(), GBC_COLUMN_A);
    add(activeProjectOpenButton, GBC_COLUMNS_BC);
    add(Box.createHorizontalGlue(), GBC_COLUMN_D);

    add(alwaysCreateWorkReportLabel, GBC_COLUMN_A);
    add(alwaysCreateWorkReportValue, GBC_COLUMNS_BCD);

    add(new JPanel(), GBC_COLUMN_A);
    add(GuiComponentFactory.createWrapperPanel(verificationNote), GBC_COLUMNS_BCD);

    add(new JPanel(), GBC_COLUMN_A);
    add(GuiComponentFactory.createWrapperPanel(feeNote), GBC_COLUMNS_BCD);

    add(inactivityToleranceLabel, GBC_COLUMN_A);
    add(inactivityToleranceValue, GBC_COLUMN_B);
    add(inactivityToleranceDescription, GBC_COLUMNS_CD);

    add(Box.createVerticalGlue(), GBC_COLUMN_A.fill(GBC.VERTICAL));
  }

  public int getActiveProjectIdValue() {
    final int value = activeProjectIdSpinnerModel.getNumber().intValue();
    return value >= RovasProperties.ACTIVE_PROJECT_ID_MIN_VALUE ? value : RovasProperties.ACTIVE_PROJECT_ID_NO_VALUE;
  }

  public void setActiveProjectIdValue(@Nullable final Integer projectId) {
    activeProjectIdSpinnerModel.setValue(
      Utils.clamp(
        Optional.ofNullable(projectId).orElse(RovasProperties.ACTIVE_PROJECT_ID_NO_VALUE),
        RovasProperties.ACTIVE_PROJECT_ID_NO_VALUE,
        Integer.MAX_VALUE
      )
    );
  }

  public boolean getAlwaysCreateWorkReport() {
    return alwaysCreateWorkReportValue.isSelected();
  }

  public void setAlwaysCreateWorkReport(final boolean value) {
    alwaysCreateWorkReportValue.setSelected(value);
  }

  public String getStringFieldValue(final Supplier<JTextField> fieldSupplier) {
    return Optional.of(fieldSupplier.get())
      .map(JTextField::getText)
      .map(String::trim)
      .filter(it -> !it.isEmpty())
      .orElse(null);
  }

  public void setStringFieldValue(final Supplier<JTextField> fieldSupplier, final String newValue) {
    fieldSupplier.get().setText(Optional.ofNullable(newValue).map(String::trim).orElse(null));
  }

  public String getApiKeyValue() {
    return getStringFieldValue(() -> apiKeyField);
  }

  public void setApiKeyValue(final String apiKey) {
    setStringFieldValue(() -> apiKeyField, apiKey);
  }

  public String getApiTokenValue() {
    return getStringFieldValue(() -> apiTokenField);
  }

  public void setApiTokenValue(final String apiToken) {
    setStringFieldValue(() -> apiTokenField, apiToken);
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
