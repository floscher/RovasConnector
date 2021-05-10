package app.rovas.josm;

import java.awt.GridBagLayout;
import java.net.URI;
import java.util.function.Supplier;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import app.rovas.josm.util.JMultilineLinkedLabel;
import app.rovas.josm.util.URIs;
import com.drew.lang.annotations.NotNull;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.OpenBrowser;

public final class RovasPreferencePanel extends VerticallyScrollablePanel {

  private static final GBC GBC_COLUMN_A = fixedToColumn(0, GBC.std().insets(5).span(1).anchor(GBC.LINE_END));
  private static final GBC GBC_COLUMNS_BC = fixedToColumn(1, GBC.eol().insets(5).span(2).fill(GBC.HORIZONTAL).anchor(GBC.LINE_START));
  private static final GBC GBC_COLUMN_B = fixedToColumn(1, GBC.std().insets(5).span(1).fill(GBC.HORIZONTAL).anchor(GBC.LINE_START));
  private static final GBC GBC_COLUMN_C = fixedToColumn(2, GBC.eol().insets(5).span(1).anchor(GBC.LINE_START));

  /**
   * Modifies GridBagConstraints, so that {@link java.awt.GridBagConstraints#gridx} is set to {@code columnIndex}.
   *
   * @param columnIndex the index of the column where the cell should be constrained to
   * @param gbc         the constraints that should be modified
   * @return the object that was passed as parameter {@code gbc}, modified to have {@code gridx} set to the value {@code columnIndex}.
   */
  @NotNull
  private static GBC fixedToColumn(final int columnIndex, @NotNull final GBC gbc) {
    gbc.gridx = columnIndex;
    return gbc;
  }

  private final JLabel apiKeyLabel = new JLabel(I18n.tr("API key"));
  private final JTextField apiKeyField = new JPasswordField();

  private final JLabel apiTokenLabel = new JLabel(I18n.tr("API token"));
  private final JTextField apiTokenField = new JPasswordField();

  private final JEditorPane seeProfilePageNote = new JMultilineLinkedLabel(
    I18n.tr("<html>Values for these fields can be found on your {0}.</html>", String.format("<a href=\"%s\">%s</a>", URIs.USER_PROFILE, I18n.tr("Rovas profile page")))
  );

  private final JLabel activeProjectIdLabel = new JLabel(I18n.tr("Project ID"));
  private final SpinnerNumberModel activeProjectIdSpinnerModel = new SpinnerNumberModel(-1, -1, Integer.MAX_VALUE, 1);
  private final JSpinner activeProjectIdSpinner = new JSpinner(activeProjectIdSpinnerModel);
  private URI activeProjectURI = null;
  private final JButton activeProjectOpenButton = new JButton(I18n.tr("Open the project page in Rovas"));

  private final URI connectorURI = URIs.project(RovasProperties.ROVAS_CONNECTOR_PROJECT_ID);
  private final JEditorPane feeNote = new JMultilineLinkedLabel(
    "<html>" +
      // i18n: {0} will be replaced by a decimal number with 2 decimal places
      // i18n: {1} will be replaced by a link labeled with "Rovas connector"
      I18n.tr(
        "Please note: A fee equal to {0}% of the amount you earn for your reports will be charged to you and used to reward the {1} project.",
        String.format("%.2f", RovasProperties.ASSET_USAGE_FEE * 100),
        connectorURI == null
          ? I18n.tr("Rovas connector")
          : String.format("<a href=\"%s\">%s</a>", connectorURI, I18n.tr("Rovas connector").replace(" ", "&nbsp;"))
      ) +
    "</html>"
  );

  public RovasPreferencePanel() {
    super();

    // Set number format of spinner (no thousands separator)
    activeProjectIdSpinner.setEditor(new JSpinner.NumberEditor(activeProjectIdSpinner, "#"));
    activeProjectIdSpinnerModel.addChangeListener((changeEvent) -> {
      final int currentActiveProjectId = activeProjectIdSpinnerModel.getNumber().intValue();
      if (currentActiveProjectId >= 1) {
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
  }

  private void buildGui() {
    setLayout(new GridBagLayout());

    add(apiKeyLabel, GBC_COLUMN_A);
    add(apiKeyField, GBC_COLUMNS_BC);

    add(apiTokenLabel, GBC_COLUMN_A);
    add(apiTokenField, GBC_COLUMNS_BC);

    add(GBC.glue(1, 1), GBC_COLUMN_A);
    add(seeProfilePageNote, GBC_COLUMNS_BC);

    add(activeProjectIdLabel, GBC_COLUMN_A);
    add(activeProjectIdSpinner, GBC_COLUMN_B);
    add(activeProjectOpenButton, GBC_COLUMN_C);

    add(GBC.glue(1, 1), GBC_COLUMN_A);
    add(feeNote, GBC_COLUMNS_BC);

    add(Box.createVerticalGlue(), GBC_COLUMN_A.fill(GBC.VERTICAL));
  }

  public int getActiveProjectIdValue() {
    final int activeProjectId = activeProjectIdSpinnerModel.getNumber().intValue();
    if (activeProjectId <= 0) {
      return -1;
    }
    return activeProjectId;
  }

  public void setActiveProjectIdValue(final int projectId) {
    activeProjectIdSpinnerModel.setValue(projectId <= 0 ? -1 : projectId);
  }

  public String getStringFieldValue(final Supplier<JTextField> fieldSupplier) {
    final String value = fieldSupplier.get().getText().trim();
    return value.isEmpty() ? null : value;
  }

  public void setStringFieldValue(final Supplier<JTextField> fieldSupplier, final String newValue) {
    fieldSupplier.get().setText(newValue == null ? null : newValue.trim());
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
}
