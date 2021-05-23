package app.rovas.josm.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Optional;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import com.drew.lang.annotations.NotNull;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

import app.rovas.josm.RovasPlugin;
import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.util.GuiComponentFactory;
import app.rovas.josm.util.I18nStrings;
import app.rovas.josm.util.RovasProperties;

public class CreateRovasReportDialog extends JDialog {

  private final Optional<ApiCredentials> apiCredentials;

  private final SpinnerNumberModel hoursModel;
  private final SpinnerNumberModel minutesModel;

  private final JButton submitReportButton = new JButton();

  private static final GBC DEFAULT_GBC = GBC.eol().anchor(GBC.LINE_START).fill(GBC.HORIZONTAL).insets(5);

  public CreateRovasReportDialog(final Optional<Changeset> changeset, final long defaultReportedSeconds) {
    super(MainApplication.getMainFrame(), I18n.tr("Create work report"), true);

    this.apiCredentials = RovasProperties.getApiCredentials();

    final long defaultReportedMinutes = (defaultReportedSeconds + 30) / 60;

    hoursModel = new SpinnerNumberModel(Math.max(0, (int) Math.min(Integer.MAX_VALUE, defaultReportedMinutes / 60)), 0, Integer.MAX_VALUE, 1);
    minutesModel = new SpinnerNumberModel((int) (defaultReportedMinutes % 60), 0, 59, 1);

    onTimeChange();
    hoursModel.addChangeListener(__ -> this.onTimeChange());
    minutesModel.addChangeListener(__ -> this.onTimeChange());

    add(new JLabel(I18n.tr("Create work report")));
    setContentPane(buildGui());
    setMaximumSize(new Dimension(
      GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width / 2,
      GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height
    ));
    pack();
    setResizable(false);
    setLocationRelativeTo(getParent());
    setVisible(true);
  }

  private JComponent buildGui() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JLabel heading = new JLabel(I18n.tr("Creating a work report in Rovas"));
    heading.setFont(heading.getFont().deriveFont(Font.BOLD, heading.getFont().getSize() * 1.8F));
    heading.setIcon(RovasPlugin.LOGO.setSize(heading.getFont().getSize(), heading.getFont().getSize()).get());
    panel.add(heading, DEFAULT_GBC.insets(10, 10, 10, 5));

    panel.add(GuiComponentFactory.createLabel(I18n.tr("Reported labor time:"), false), DEFAULT_GBC.insets(10, 5, 10, 0));
    final JPanel timeFieldsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    timeFieldsPanel.add(GuiComponentFactory.createSpinner(hoursModel, 3, true));
    timeFieldsPanel.add(GuiComponentFactory.createLabel(I18nStrings.trShorthandForHours(), false));
    timeFieldsPanel.add(GuiComponentFactory.createSpinner(minutesModel, 3, true, "00"));
    timeFieldsPanel.add(GuiComponentFactory.createLabel(I18nStrings.trShorthandForMinutes(), false));
    panel.add(timeFieldsPanel, DEFAULT_GBC.insets(10, 5, 10, 5));

    panel.add(new JMultilineLabel(
      I18n.tr("Click the button below to create a report in Rovas for the time shown.<br>After approving the time by two Rovas-selected users, you will earn 1 chron for every 6 minutes.")),
      DEFAULT_GBC.insets(10, 5, 10, 5)
    );

    submitReportButton.setIcon(ImageProvider.get("misc/statusreport", ImageProvider.ImageSizes.SIDEBUTTON));
    submitReportButton.addActionListener(e -> {
      setVisible(false);
      @NotNull final ApiCredentials apiCredentials;
      if (!this.apiCredentials.isPresent()) {
        final ApiCredentialsPanel apiCredentialsPanel = new ApiCredentialsPanel();
        Optional<ApiCredentials> newApiCredentials = Optional.empty();
        while (!newApiCredentials.isPresent()) {
          if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(MainApplication.getMainFrame(), apiCredentialsPanel, "Enter API credentials", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)) {
            newApiCredentials = ApiCredentials.createFrom(
              apiCredentialsPanel.getApiKeyValue(),
              apiCredentialsPanel.getApiTokenValue(),
              apiCredentialsPanel.getActiveProjectIdValue()
            );
          } else {
            setVisible(true);
            return;
          }
        }
        RovasProperties.persistApiCredentials(apiCredentialsPanel);
        apiCredentials = newApiCredentials.get();
      } else {
        apiCredentials = this.apiCredentials.get();
      }

      JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "Creating a report for project " + apiCredentials.getProjectId() + " is currently not implement, this is coming soon!", "Coming soon", JOptionPane.INFORMATION_MESSAGE);
    });
    panel.add(GuiComponentFactory.createWrapperPanel(submitReportButton, new FlowLayout(FlowLayout.LEFT)), DEFAULT_GBC.insets(10, 5, 10, 0));
    panel.add(new JCheckBox("Remember this choice"), DEFAULT_GBC.insets(10, 0, 10, 5));

    panel.add(GuiComponentFactory.createHyperlinkedMultilineLabel(I18nStrings.trVerificationWarningWithHyperlink()), DEFAULT_GBC.insets(10, 5, 10, 5));

    final JButton skipButton = new JButton(new AbstractAction(I18n.tr("Skip without creating a report"), ImageProvider.get("dialogs/next", ImageProvider.ImageSizes.SIDEBUTTON)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });
    skipButton.setHorizontalTextPosition(SwingConstants.LEFT);
    panel.add(skipButton, GBC.eol().anchor(GBC.LINE_END).insets(10));

    return panel;
  }

  private void onTimeChange() {
    GuiHelper.runInEDT(() -> {
      final long minutes = 60 * hoursModel.getNumber().longValue() + minutesModel.getNumber().longValue();
      submitReportButton.setText(I18n.tr("Submit report for {0} minutes", minutes));
      submitReportButton.setEnabled(minutes > 0);
    });
  }
}
