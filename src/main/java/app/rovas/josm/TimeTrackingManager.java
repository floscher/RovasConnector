package app.rovas.josm;

import java.time.Instant;
import java.util.Optional;

import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.gui.layer.LayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

public final class TimeTrackingManager {

  private final ListenerList<TimeTrackingUpdateListener> listeners = ListenerList.create();

  private static final TimeTrackingManager INSTANCE = new TimeTrackingManager();
  private static final DataSetListenerAdapter DATASET_LISTENER_ADAPTER = new DataSetListenerAdapter(__ -> INSTANCE.trackChangeNow());

  private long committedSeconds = 0;

  private Long firstUncommittedChangeTimestamp = null;
  private Long lastUncommittedChangeTimestamp = null;

  private TimeTrackingManager() {
    // private constructor to avoid instantiation
  }

  public void addAndFireTimeTrackingUpdateListener(final TimeTrackingUpdateListener listener) {
    listeners.addListener(listener);
    fireTimeTrackingUpdateListeners();
  }

  private void fireTimeTrackingUpdateListeners() {
    listeners.fireEvent(it -> it.updateNumberOfTrackedSeconds(
      committedSeconds +
      Optional.ofNullable(firstUncommittedChangeTimestamp)
        .flatMap(first ->
          Optional.ofNullable(lastUncommittedChangeTimestamp)
            .map(last -> last - first)
        )
        .orElse(0L)
    ));
  }

  public void removeTimeTrackingUpdateListener(final TimeTrackingUpdateListener listener) {
    listeners.removeListener(listener);
  }

  /**
   * @return the singleton instance of the {@link TimeTrackingManager}
   */
  public static TimeTrackingManager getInstance() {
    return INSTANCE;
  }

  /**
   * Whenever this method is called, a change is tracked for the current timestamp.
   * This is equivalent to calling {@link #trackChangeAt(Instant)} with {@link Instant#now()} as argument.
   */
  public void trackChangeNow() {
    trackChangeAt(Instant.now());
  }

  /**
   * Registers the given instant with the time tracker. This should be called whenever a change occurs.
   *
   * Usually you should use {@link #trackChangeNow()} instead. This method exists
   * for easier testing with arbitrary timestamps.
   *
   * <p>
   * <strong>Note:</strong> We expect to receive the calls to this method in order.
   * If an instant A is registered later than instant B (where B has a higher unix timestamp than A),
   * then an error is logged. It's handled gracefully, but should be avoided.
   * This should really only happen if the user manually set their clock to an earlier time.
   * </p>
   *
   * @param instant the timestamp when the change occured. The seconds of that instant are recorded.
   */
  protected synchronized void trackChangeAt(final Instant instant) {
    final int tolerance = Math.max(0, RovasProperties.INACTIVITY_TOLERANCE.get());
    final Long firstTimestamp = firstUncommittedChangeTimestamp;
    final Long lastTimestamp = lastUncommittedChangeTimestamp;
    final long currentTimestamp = instant.getEpochSecond();

    Logging.debug(String.format("Time tracker received new change at %d: uncommitted interval: %d - %d", currentTimestamp, firstTimestamp, lastTimestamp));

    if (firstTimestamp == null) {
      // initialize when no time was tracked before
      resetAutomaticTracker(currentTimestamp, false);
    } else if (lastTimestamp != null) {
      // something went wrong, current time is before the last timestamp that was recorded previously.
      if (currentTimestamp < lastTimestamp) {
        Logging.error("Your clock seems to have been running backwards!");
      }
      // In case the current time is not inside the uncommitted timespan (including tolerance), then that timespan is committed and a new timespan is started.
      if (currentTimestamp > lastTimestamp + tolerance || currentTimestamp < firstTimestamp) {
        Logging.debug("Committing {0} (+ {1}) seconds to time tracker", lastTimestamp - firstTimestamp, tolerance);
        resetAutomaticTracker(currentTimestamp, true);
      } else {
        // if inside tolerance, extend the current uncommitted timespan
        lastUncommittedChangeTimestamp = Math.max(lastTimestamp, currentTimestamp);
      }
    }

    fireTimeTrackingUpdateListeners();
  }

  public synchronized void setCurrentlyTrackedSeconds(final int numSeconds) {
    resetAutomaticTracker(null, false);
    this.committedSeconds = numSeconds;
    fireTimeTrackingUpdateListeners();
  }

  public long commit() {
    return resetAutomaticTracker(null, true);
  }

  /**
   *
   * @param newValue
   * @param commitUncommittedTime
   * @return the current number of tracked seconds, after the reset
   */
  private long resetAutomaticTracker(final Long newValue, final boolean commitUncommittedTime) {
    if (commitUncommittedTime) {
      final Long firstUncommitted = this.firstUncommittedChangeTimestamp;
      final Long lastUncommitted = this.lastUncommittedChangeTimestamp;
      if (firstUncommitted != null && lastUncommitted != null) {
        this.committedSeconds +=
          Math.max(0, lastUncommitted - firstUncommitted) + // uncommitted time
            Math.min(
              Instant.now().getEpochSecond() - lastUncommitted, // time since last uncommitted timestamp
              Math.max(0, RovasProperties.INACTIVITY_TOLERANCE.get()) // tolerance
            );
      }
    }
    this.firstUncommittedChangeTimestamp = newValue;
    this.lastUncommittedChangeTimestamp = newValue;
    return this.committedSeconds;
  }

  /**
   * This listener notifies the (singleton) {@link TimeTrackingManager} of all changes in any {@link OsmDataLayer}.
   * Add this to JOSM's {@link LayerManager} using
   * {@link LayerManager#addAndFireLayerChangeListener(LayerManager.LayerChangeListener)}.
   */
  public static class AnyOsmDataChangeListener implements LayerManager.LayerChangeListener {
    @Override
    public void layerAdded(LayerManager.LayerAddEvent e) {
      INSTANCE.trackChangeNow();
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
