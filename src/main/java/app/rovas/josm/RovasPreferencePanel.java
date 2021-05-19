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
import app.rovas.josm.util.URIs;

public final class RovasPreferencePanel extends VerticallyScrollablePanel {

  private static final GBC GBC_COLUMN_A = GBCUtil.fixedToColumn(0, GBC.std().insets(5).span(1).anchor(GBC.LINE_END));
  private static final GBC GBC_COLUMNS_BC = GBCUtil.fixedToColumn(1, GBC.eol().insets(5).span(2).fill(GBC.HORIZONTAL).anchor(GBC.FIRST_LINE_START));
  private static final GBC GBC_COLUMN_B = GBCUtil.fixedToColumn(1, GBC.std().insets(5).span(1).anchor(GBC.LINE_START));
  private static final GBC GBC_COLUMN_C = GBCUtil.fixedToColumn(2, GBC.eol().insets(5).span(1).fill(GBC.HORIZONTAL).anchor(GBC.LINE_START));

  private final JLabel apiKeyLabel = new JLabel(I18n.tr("API key"));
  private final JTextField apiKeyField = new JPasswordField();

  private final JLabel apiTokenLabel = new JLabel(I18n.tr("API token"));
  private final JTextField apiTokenField = new JPasswordField();

  private final JEditorPane seeProfilePageNote = GuiComponentFactory.createHyperlinkedMultilineLabel(
    "<html>" +
    I18n.tr(
      "Values for these fields can be found on your {0}.",
      URIs.toHtmlHyperlink(URIs.USER_PROFILE, I18n.tr("Rovas profile page"))
    ) +
    "</html>"
  );

  private final JLabel activeProjectIdLabel = new JLabel(I18n.tr("Project ID"));
  private final SpinnerNumberModel activeProjectIdSpinnerModel = new SpinnerNumberModel(RovasProperties.ACTIVE_PROJECT_ID_NO_VALUE, RovasProperties.ACTIVE_PROJECT_ID_NO_VALUE, Integer.MAX_VALUE, 1);
  private final JSpinner activeProjectIdSpinner = GuiComponentFactory.createSpinner(activeProjectIdSpinnerModel, 5);
  private URI activeProjectURI = null;
  private final JButton activeProjectOpenButton = new JButton(I18n.tr("Open the project page in Rovas"));
  private final JLabel activeProjectIdDescription = GuiComponentFactory.createLabel(I18n.tr("(the parent project of the created work reports)"), false);

  private final JLabel alwaysCreateWorkReportLabel = new JLabel(I18n.tr("Work reports"));
  private final JCheckBox alwaysCreateWorkReportValue = new JCheckBox(I18n.tr("Always create a work report by default when uploading OSM data"));

  private final JEditorPane verificationNote = GuiComponentFactory.createHyperlinkedMultilineLabel(
    "<p><span style='color:#ff0000'>" +
    I18n.tr("<strong>Please note,</strong> by creating a work report you also agree to verify (typically) two work reports submitted by other users for each of your submitted work reports.") +
    "</span> " +
    // i18n: {0} is replaced by a link labeled "rules page in Rovas"
    I18n.tr(
      "See the {0} for more information",
      URIs.toHtmlHyperlink(URIs.RULES, I18n.tr("rules page in Rovas"))
    ) +
    "</p>"
  );

  private final URI connectorURI = URIs.project(RovasProperties.ROVAS_CONNECTOR_PROJECT_ID);
  private final JEditorPane feeNote = GuiComponentFactory.createHyperlinkedMultilineLabel(
    "<html>" +
      // i18n: {0} will be replaced by a decimal number with 2 decimal places
      // i18n: {1} will be replaced by a link labeled with "Rovas connector"
      I18n.tr(
        "To reward the authors of this {1}, a fee equal to {0}% of the amount you earn from reports created by the plugin will be levied on those earnings.",
        String.format("%.2f", RovasProperties.ASSET_USAGE_FEE * 100),
        String.format("<a href=\"%s\">%s</a>", connectorURI == null ? "#" : connectorURI, I18n.tr("Rovas connector plugin").replace(" ", "&nbsp;"))
      ) +
    "</html>"
  );

  private final JLabel inactivityToleranceLabel = new JLabel(I18n.tr("Automatic change detection"));
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
  private final JSpinner inactivityToleranceValue = GuiComponentFactory.createSpinner(inactivityToleranceModel, 5);
  private final JLabel inactivityToleranceDescription = GuiComponentFactory.createLabel(I18n.tr("seconds before each change are counted as work time"), false);

  public RovasPreferencePanel() {
    super();

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
    add(apiTokenLabel, GBC_COLUMN_A);
    add(apiTokenField, GBC_COLUMNS_BC);

    add(GBC.glue(1, 1), GBC_COLUMN_A);
    add(seeProfilePageNote, GBC_COLUMNS_BC);
    add(GBC.glue(1, 1), GBC_COLUMN_A);
    add(GBC.glue(1, 1), GBC_COLUMNS_BC);

    add(activeProjectIdLabel, GBC_COLUMN_A);
    add(activeProjectIdSpinner, GBC_COLUMN_B);
    add(activeProjectIdDescription, GBC_COLUMN_C);
    add(GBC.glue(1, 1), GBC_COLUMN_A);
    add(activeProjectOpenButton, GBC_COLUMNS_BC);
    add(GBC.glue(1, 1), GBC_COLUMN_A);
    add(GBC.glue(1, 1), GBC_COLUMNS_BC);

    add(alwaysCreateWorkReportLabel, GBC_COLUMN_A);
    add(alwaysCreateWorkReportValue, GBC_COLUMNS_BC);

    add(GBC.glue(1, 1), GBC_COLUMN_A);
    add(verificationNote, GBC_COLUMNS_BC);

    add(GBC.glue(1, 1), GBC_COLUMN_A);
    add(feeNote, GBC_COLUMNS_BC);
    add(GBC.glue(1, 1), GBC_COLUMN_A);
    add(GBC.glue(1, 1), GBC_COLUMNS_BC);

    add(inactivityToleranceLabel, GBC_COLUMN_A);
    add(inactivityToleranceValue, GBC_COLUMN_B);
    add(inactivityToleranceDescription, GBC_COLUMN_C);

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
