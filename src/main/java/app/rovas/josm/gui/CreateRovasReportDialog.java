// License: GPL. For details, see LICENSE file.
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
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

import app.rovas.josm.RovasPlugin;
import app.rovas.josm.gui.upload.UploadStep1AddShareholder;
import app.rovas.josm.model.TimeTrackingManager;
import app.rovas.josm.util.GuiComponentFactory;
import app.rovas.josm.util.I18nStrings;
import app.rovas.josm.util.TimeConverterUtil;
import app.rovas.josm.util.UrlProvider;

/**
 * The dialog that asks the user, if a work report should be created.
 * The time reported to Rovas can be modified before submitting.
 */
public class CreateRovasReportDialog extends JDialog {
  private static final GBC GBC_START_ALIGNED = GBC.eol().anchor(GBC.LINE_START).fill(GBC.HORIZONTAL);

  private final SpinnerNumberModel hoursModel;
  private final SpinnerNumberModel minutesModel;

  private final JMultilineLabel calculatedChronLabel = new JMultilineLabel("");
  private final JButton submitReportButton = new JButton(
    new AbstractAction(I18n.tr("Submit report"), ImageProvider.get("upload", ImageProvider.ImageSizes.SIDEBUTTON)) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        new UploadStep1AddShareholder(
          hoursModel.getNumber().intValue() * 60 + minutesModel.getNumber().intValue(),
          changeset
        ).showStep(
          CreateRovasReportDialog.this,
          UrlProvider.getInstance(),
          CreateRovasReportDialog.this.timeTrackingManager
        );
      }
    }
  );

  private final transient Optional<Changeset> changeset;
  private final TimeTrackingManager timeTrackingManager;

  /**
   * The dialog that is shown after an OSM upload completed. With it, the user can correct the recorded time
   * and then submit the time to Rovas
   * @param changeset the OSM {@link Changeset} for which the work report will be created. Usually present, but could be empty.
   * @param defaultReportedSeconds the time (in seconds) that should be shown initially in the dialog, before the user edits it
   */
  public CreateRovasReportDialog(final TimeTrackingManager timeTrackingManager, final Optional<Changeset> changeset, final long defaultReportedSeconds) {
    super(MainApplication.getMainFrame(), I18n.tr("Create work report"), true);

    final int defaultReportedMinutes = Math.min(Integer.MAX_VALUE, TimeConverterUtil.secondsToMinutes(defaultReportedSeconds));

    this.timeTrackingManager = timeTrackingManager;
    this.changeset = changeset;
    this.hoursModel = new SpinnerNumberModel(Utils.clamp(defaultReportedMinutes / 60, 0, TimeConverterUtil.MAX_HOURS), 0, TimeConverterUtil.MAX_HOURS, 1);
    this.minutesModel = new SpinnerNumberModel(defaultReportedMinutes % 60, 0, 59, 1);

    onTimeChange();
    hoursModel.addChangeListener(__ -> this.onTimeChange());
    minutesModel.addChangeListener(__ -> this.onTimeChange());

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
    panel.add(heading, GBC_START_ALIGNED.insets(10, 10, 10, 5));

    panel.add(GuiComponentFactory.createLabel(I18n.tr("Reported labor time"), false, JLabel.CENTER), GBC_START_ALIGNED.insets(10, 5, 10, 0));
    panel.add(
      GuiComponentFactory.createWrapperPanel(
        new FlowLayout(FlowLayout.CENTER),
        GuiComponentFactory.createSpinner(hoursModel, 5, true),
        GuiComponentFactory.createLabel(I18nStrings.trShorthandForHours(), false),
        GuiComponentFactory.createSpinner(minutesModel, 2, true, "00"),
        GuiComponentFactory.createLabel(I18nStrings.trShorthandForMinutes(), false)
      ),
      GBC_START_ALIGNED.insets(10, 5, 10, 5)
    );

    panel.add(calculatedChronLabel, GBC_START_ALIGNED.insets(10, 5, 10, 5));

    panel.add(GuiComponentFactory.createHyperlinkedMultilineLabel(I18nStrings.trVerificationWarningWithHyperlink()), GBC_START_ALIGNED.insets(10, 5, 10, 5));

    panel.add(
      GuiComponentFactory.createWrapperPanel(
        new FlowLayout(FlowLayout.CENTER, 10, 10),
        submitReportButton,
        new JButton(new AbstractAction(I18n.tr("Cancel"), ImageProvider.get("cancel", ImageProvider.ImageSizes.SIDEBUTTON)) {
          @Override
          public void actionPerformed(final ActionEvent e) {
            dispose();
            if (changeset.isPresent()) {
              // Reset the timer if there is a changeset but no work report created
              timeTrackingManager.setCurrentlyTrackedSeconds(0);
            }
          }
        })
      ),
      GBC_START_ALIGNED.insets(10)
    );

    return panel;
  }

  private void onTimeChange() {
    GuiHelper.runInEDT(() -> {
      final int minutes = 60 * hoursModel.getNumber().intValue() + minutesModel.getNumber().intValue();
      calculatedChronLabel.setText(I18n.tr("After approving the time by two Rovas-selected users, you will earn {0,number,#.##} chrons", TimeConverterUtil.minutesToChrons(minutes)));
      submitReportButton.setEnabled(minutes > 0);
    });
  }
}
