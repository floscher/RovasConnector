package app.rovas.josm;

import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.ImageProvider;

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
    TimeTrackingManager.enableListeningForAllOsmDataChanges();
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
