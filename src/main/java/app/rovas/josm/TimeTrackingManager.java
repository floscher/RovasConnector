package app.rovas.josm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.LayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Utils;

public final class TimeTrackingManager extends DataSetListenerAdapter implements DataSetListenerAdapter.Listener {

  private final CopyOnWriteArrayList<TimeTrackingUpdateListener> listeners = new CopyOnWriteArrayList<>();

  private static final TimeTrackingManager INSTANCE = new TimeTrackingManager();
  private static final DataSetListenerAdapter DATASET_LISTENER_ADAPTER = new DataSetListenerAdapter(INSTANCE);

  // TODO: Improve this, to limit how much accumulates
  private final List<Long> timestamps = new ArrayList<>();

  private TimeTrackingManager() {
    super(INSTANCE);
    // private constructor to avoid instantiation
  }

  public void addAndFireTimeTrackingUpdateListener(final TimeTrackingUpdateListener listener) {
    listeners.addIfAbsent(listener);
    fireTimeTrackingUpdateListeners();
  }

  private void fireTimeTrackingUpdateListeners() {
    listeners.forEach(it -> it.updateNumberOfTrackedChanges(timestamps.size()));
  }

  public void removeTimeTrackingUpdateListener(final TimeTrackingUpdateListener listener) {
    listeners.remove(listener);
  }

  public static TimeTrackingManager getInstance() {
    return INSTANCE;
  }

  @Override
  public void processDatasetEvent(AbstractDatasetChangedEvent event) {
    trackInstant(Instant.now());
  }

  private synchronized void trackInstant(final Instant instant) {
    timestamps.add(instant.toEpochMilli());
    fireTimeTrackingUpdateListeners();
  }

  /**
   * When this method is called, all {@link OsmDataLayer}s that exist at that moment and all {@link OsmDataLayer}s
   * that are added later will call {@link #processDatasetEvent(AbstractDatasetChangedEvent)} for
   * all changes that are made to those layers.
   */
  public static void enableListeningForAllOsmDataChanges() {
    MainApplication.getLayerManager().addAndFireLayerChangeListener(new AnyOsmDataChangeListener());
  }

  /**
   * This listener notifies the (singleton) {@link TimeTrackingManager} of any changes in any {@link OsmDataLayer}.
   */
  private static class AnyOsmDataChangeListener implements LayerManager.LayerChangeListener {
    @Override
    public void layerAdded(LayerManager.LayerAddEvent e) {
      Utils.instanceOfAndCast(e.getAddedLayer(), OsmDataLayer.class)
        .ifPresent(layer -> layer.data.addDataSetListener(DATASET_LISTENER_ADAPTER));
    }
    @Override
    public void layerRemoving(LayerManager.LayerRemoveEvent e) {
      Utils.instanceOfAndCast(e.getRemovedLayer(), OsmDataLayer.class)
        .ifPresent(layer -> layer.data.removeDataSetListener(DATASET_LISTENER_ADAPTER));
    }
    @Override
    public void layerOrderChanged(LayerManager.LayerOrderChangeEvent e) {
      // do nothing
    }
  }
}
