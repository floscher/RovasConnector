package app.rovas.josm;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Optional;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmServerWriter;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.ImageProvider;

import app.rovas.josm.gui.CreateRovasReportDialog;
import app.rovas.josm.gui.RovasDialog;
import app.rovas.josm.model.RovasPreference;
import app.rovas.josm.model.TimeTrackingManager;

public final class RovasPlugin extends Plugin {
  public static final ImageProvider LOGO = new ImageProvider("rovas_logo");

  public static final RovasPreference PREFERENCE = new RovasPreference();

  /**
   * Creates the plugin
   *
   * @param info the plugin information describing the plugin.
   */
  public RovasPlugin(PluginInformation info) {
    super(info);
    MainApplication.getLayerManager().addAndFireLayerChangeListener(new TimeTrackingManager.AnyOsmDataChangeListener());
    TimeTrackingManager.getInstance().trackChangeNow();
    OsmServerWriter.registerPostprocessor((p, progress) ->
      new Thread(() -> new CreateRovasReportDialog(
        Optional.ofNullable(OsmApi.getOsmApi()).map(OsmApi::getChangeset),
        TimeTrackingManager.getInstance().commit()
      ))
    );

    MainApplication.getMainFrame().getMenu().add(createRovasMenu());
  }

  /**
   *
   * @deprecated will be removed for production
   */
  @Deprecated
  private JMenu createRovasMenu() {
    final JMenu rovasMenu = new JMenu("Rovas");

    final JMenuItem description = new JMenuItem("This menu will NOT be present in the production version!");
    description.setFont(description.getFont().deriveFont(Font.PLAIN));
    description.setEnabled(false);
    rovasMenu.add(description);

    final JMenuItem triggerReportItem = new JMenuItem(new AbstractAction("Open the work report upload dialog") {
      @Override
      public void actionPerformed(ActionEvent e) {
        new CreateRovasReportDialog(Optional.empty(), TimeTrackingManager.getInstance().commit());
        TimeTrackingManager.getInstance().setCurrentlyTrackedSeconds(0);
      }
    });
    rovasMenu.add(triggerReportItem);
    return rovasMenu;
  }

  @Override
  public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
    super.mapFrameInitialized(oldFrame, newFrame);
    if (newFrame != null && newFrame.getToggleDialog(RovasDialog.class) == null) {
      newFrame.addToggleDialog(new RovasDialog());
    }
  }

  @Override
  public PreferenceSetting getPreferenceSetting() {
    return PREFERENCE;
  }
}
