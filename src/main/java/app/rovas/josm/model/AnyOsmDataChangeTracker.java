package app.rovas.josm.model;

import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.gui.layer.LayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Utils;

/**
 * <p>This listener notifies the given {@link TimeTrackingManager} of all changes in any {@link OsmDataLayer}
 * that are relevant for time tracking.</p>
 * <p>Add this to JOSM's {@link LayerManager} using
 * {@link LayerManager#addAndFireLayerChangeListener(LayerManager.LayerChangeListener)}.</p>
 */
public class AnyOsmDataChangeTracker implements LayerManager.LayerChangeListener {

  private final TimeTrackingManager timeTrackingManager;
  private final DataSetListener datasetListener;

  /**
   * Creates a new listener that will track any change in OSM data to the given time tracking manager
   * @param timeTrackingManager the time tracking manager to report changes to
   */
  public AnyOsmDataChangeTracker(final TimeTrackingManager timeTrackingManager) {
    this.timeTrackingManager = timeTrackingManager;
    this.datasetListener = new DataSetListenerAdapter(__ -> timeTrackingManager.trackChangeNow());
  }

  @Override
  public void layerAdded(final LayerManager.LayerAddEvent e) {
    timeTrackingManager.trackChangeNow();
    Utils.instanceOfAndCast(e.getAddedLayer(), OsmDataLayer.class)
      .ifPresent(layer -> layer.data.addDataSetListener(datasetListener));
  }

  @Override
  public void layerRemoving(final LayerManager.LayerRemoveEvent e) {
    Utils.instanceOfAndCast(e.getRemovedLayer(), OsmDataLayer.class)
      .ifPresent(layer -> layer.data.removeDataSetListener(datasetListener));
  }

  @Override
  public void layerOrderChanged(final LayerManager.LayerOrderChangeEvent e) {
    // do nothing
  }
}
