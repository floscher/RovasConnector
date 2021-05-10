package app.rovas.josm;

import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.ImageProvider;

public class RovasPlugin extends Plugin {
  public static final ImageProvider LOGO = new ImageProvider("rovas_logo");

  public static final RovasDialog DIALOG = new RovasDialog();

  public static final RovasPreference PREFERENCE = new RovasPreference();

  /**
   * Creates the plugin
   *
   * @param info the plugin information describing the plugin.
   */
  public RovasPlugin(PluginInformation info) {
    super(info);
    MainApplication.getLayerManager().addAndFireLayerChangeListener(new LayerManager.LayerChangeListener() {
      @Override
      public void layerAdded(LayerManager.LayerAddEvent e) {
        final Layer l = e.getAddedLayer();
        if (l instanceof OsmDataLayer) {
          ((OsmDataLayer) l).data.addDataSetListener(new DataSetListenerAdapter(DIALOG));
        }
      }

      @Override
      public void layerRemoving(LayerManager.LayerRemoveEvent e) {
      }

      @Override
      public void layerOrderChanged(LayerManager.LayerOrderChangeEvent e) {
        // do nothing
      }
    });
  }

  @Override
  public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
    super.mapFrameInitialized(oldFrame, newFrame);
    if (newFrame != null && newFrame.getToggleDialog(RovasDialog.class) == null) {
      newFrame.addToggleDialog(DIALOG);
    }
  }

  @Override
  public PreferenceSetting getPreferenceSetting() {
    return PREFERENCE;
  }
}
