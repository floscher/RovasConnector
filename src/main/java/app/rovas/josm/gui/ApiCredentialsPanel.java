package app.rovas.josm.gui;

import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;

import com.drew.lang.annotations.Nullable;

import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Utils;

import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.util.GBCUtil;
import app.rovas.josm.util.GuiComponentFactory;
import app.rovas.josm.model.RovasProperties;
import app.rovas.josm.util.UrlProvider;

public class ApiCredentialsPanel extends VerticallyScrollablePanel {

  protected static final GBC GBC_COLUMN_A = GBCUtil.fixedToColumn(0,
    GBC.std().insets(5).span(1).anchor(GBC.LINE_END)
  );
  protected static final GBC GBC_COLUMNS_ABCD = GBCUtil.fixedToColumn(0,
    GBC.std().insets(5).span(4).fill(GBC.HORIZONTAL).anchor(GBC.CENTER)
  );
  protected static final GBC GBC_COLUMNS_BC = GBCUtil.fixedToColumn(1,
    GBC.std().insets(5).span(2).fill(GBC.HORIZONTAL).anchor(GBC.LINE_START)
  );
  protected static final GBC GBC_COLUMN_B = GBCUtil.fixedToColumn(1,
    GBC.std().insets(5).span(1).fill(GBC.HORIZONTAL).weight(0.0, 0.0).anchor(GBC.LINE_START)
  );
  protected static final GBC GBC_COLUMN_D = GBCUtil.fixedToColumn(3,
    GBC.std().insets(5).span(1).fill(GBC.HORIZONTAL).anchor(GBC.LINE_START)
  );
  protected static final GBC GBC_COLUMNS_CD = GBCUtil.fixedToColumn(2,
    GBC.std().insets(5).span(2).fill(GBC.HORIZONTAL).anchor(GBC.LINE_START)
  );
  protected static final GBC GBC_COLUMNS_BCD = GBCUtil.fixedToColumn(1,
    GBC.std().insets(5).span(3).fill(GBC.HORIZONTAL).anchor(GBC.LINE_START)
  );

  private final JLabel apiKeyLabel = new JLabel(I18n.tr("API key"));
  private final JTextField apiKeyField = new JPasswordField(35);

  private final JLabel apiTokenLabel = new JLabel(I18n.tr("API token"));
  private final JTextField apiTokenField = new JPasswordField(35);

  private final JEditorPane seeProfilePageNote = GuiComponentFactory.createHyperlinkedMultilineLabel(
    "<html>" +
      I18n.tr(
        "Values for these fields can be found on your {0}.",
        UrlProvider.toHtmlHyperlink(UrlProvider.getInstance().userProfile(), I18n.tr("Rovas profile page"))
      ) +
      "</html>"
  );

  private final JLabel activeProjectIdLabel = new JLabel(I18n.tr("Project ID"));
  private final SpinnerNumberModel activeProjectIdSpinnerModel = new SpinnerNumberModel(RovasProperties.ACTIVE_PROJECT_ID_NO_VALUE, RovasProperties.ACTIVE_PROJECT_ID_NO_VALUE, Integer.MAX_VALUE, 0);
  private final JSpinner activeProjectIdSpinner = GuiComponentFactory.createSpinner(activeProjectIdSpinnerModel, 5, true);
  private final JButton activeProjectOpenButton = new JButton(I18n.tr("Open project page in Rovas"), ImageProvider.get("help", "internet", ImageProvider.ImageSizes.SIDEBUTTON));
  private final JLabel activeProjectIdDescription = GuiComponentFactory.createLabel(I18n.tr("(the parent project of the created work reports)"), false);

  private final JEditorPane validationWarning = new JMultilineLabel(
    "<html><div style='background:#fdc14b;padding:5px 10px'>" +
      I18n.tr("All fields have to be filled out!") +
    "</div></html>"
  );

  public ApiCredentialsPanel(final boolean showValidationWarning) {
    super();
    validationWarning.setVisible(false);
    if (showValidationWarning) {
      final AbstractTextComponentValidator apiKeyValidator = new NonBlankTextFieldValidator(apiKeyField);
      final AbstractTextComponentValidator apiTokenValidator = new NonBlankTextFieldValidator(apiTokenField);
      final AbstractTextComponentValidator projectIdValidator = new AbstractTextComponentValidator(((JSpinner.DefaultEditor) activeProjectIdSpinner.getEditor()).getTextField()) {
        @Override
        public void validate() {
          if (isValid()) {
            feedbackValid("");
          } else {
            feedbackInvalid(I18n.tr("The project ID must be ≥ {0}!", ApiCredentials.MIN_PROJECT_ID));
          }
        }
        @Override
        public boolean isValid() {
          return ApiCredentials.isValidProjectId(activeProjectIdSpinnerModel.getNumber().intValue());
        }
      };

      final List<AbstractTextComponentValidator> validators = Arrays.asList(apiKeyValidator, apiTokenValidator, projectIdValidator);
      validators.forEach(validator -> {
        validator.addChangeListener(__ -> {
          validationWarning.setVisible(!validators.stream().allMatch(AbstractTextComponentValidator::isValid));
          // In case the panel is embedded in a window, resize it to fit the new content
          Optional.ofNullable((Window) SwingUtilities.getAncestorOfClass(Window.class, this)).ifPresent(Window::pack);
        });
        validator.validate();
      });

      activeProjectIdSpinnerModel.addChangeListener(ce -> projectIdValidator.validate());
      apiKeyField.getDocument().addDocumentListener(apiKeyValidator);
      apiTokenField.getDocument().addDocumentListener(apiTokenValidator);
    }

    // Update the URL that the button opens
    activeProjectIdSpinnerModel.addChangeListener(
      changeEvent -> activeProjectOpenButton.setEnabled(
        ApiCredentials.isValidProjectId(activeProjectIdSpinnerModel.getNumber().intValue())
      )
    );
    activeProjectOpenButton.setEnabled(false);
    activeProjectOpenButton.addActionListener((e) -> {
      final Optional<Integer> projectId = Optional.ofNullable(activeProjectIdSpinnerModel.getNumber())
        .map(Number::intValue)
        .filter(ApiCredentials::isValidProjectId);

      if (projectId.isPresent()) {
        OpenBrowser.displayUrl(UrlProvider.getInstance().node(projectId.get()).toString());
      } else {
        activeProjectOpenButton.setEnabled(false);
        Arrays.stream(activeProjectIdSpinnerModel.getChangeListeners())
          .forEach(it -> it.stateChanged(new ChangeEvent(activeProjectOpenButton)));
      }
    });

    buildGui();

    setApiKeyValue(RovasProperties.ROVAS_API_KEY.get());
    setApiTokenValue(RovasProperties.ROVAS_API_TOKEN.get());
    setActiveProjectIdValue(RovasProperties.ACTIVE_PROJECT_ID.get());
  }

  /**
   * Sets the layout and adds all GUI components
   */
  private void buildGui() {
    setLayout(new GridBagLayout());

    add(validationWarning, GBC_COLUMNS_ABCD);

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
    add(GuiComponentFactory.createWrapperPanel(new FlowLayout(FlowLayout.LEFT, 0, 0), activeProjectOpenButton), GBC_COLUMNS_BC);
    add(Box.createHorizontalGlue(), GBC_COLUMN_D);
  }

  public int getActiveProjectIdValue() {
    return Optional.of(activeProjectIdSpinnerModel.getNumber().intValue())
      .filter(ApiCredentials::isValidProjectId)
      .orElse(RovasProperties.ACTIVE_PROJECT_ID_NO_VALUE);
  }

  public final void setActiveProjectIdValue(@Nullable final Integer projectId) {
    activeProjectIdSpinnerModel.setValue(
      Utils.clamp(
        Optional.ofNullable(projectId).orElse(RovasProperties.ACTIVE_PROJECT_ID_NO_VALUE),
        RovasProperties.ACTIVE_PROJECT_ID_NO_VALUE,
        Integer.MAX_VALUE
      )
    );
  }

  private String getStringFieldValue(final Supplier<JTextField> fieldSupplier) {
    return Optional.of(fieldSupplier.get())
      .map(JTextField::getText)
      .map(String::trim)
      .filter(it -> !it.isEmpty())
      .orElse(null);
  }

  private void setStringFieldValue(final Supplier<JTextField> fieldSupplier, final String newValue) {
    fieldSupplier.get().setText(Optional.ofNullable(newValue).map(String::trim).orElse(null));
  }

  public final String getApiKeyValue() {
    return getStringFieldValue(() -> apiKeyField);
  }

  public final void setApiKeyValue(final String apiKey) {
    setStringFieldValue(() -> apiKeyField, apiKey);
  }

  public final String getApiTokenValue() {
    return getStringFieldValue(() -> apiTokenField);
  }

  public final void setApiTokenValue(final String apiToken) {
    setStringFieldValue(() -> apiTokenField, apiToken);
  }
}
