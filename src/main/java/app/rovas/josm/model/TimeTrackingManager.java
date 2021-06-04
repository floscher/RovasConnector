package app.rovas.josm.model;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.Optional;

import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.gui.layer.LayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

import app.rovas.josm.gui.TimeTrackingUpdateListener;
import app.rovas.josm.util.LoggingUtil;
import app.rovas.josm.util.RovasProperties;
import app.rovas.josm.util.VisibleForTesting;

public final class TimeTrackingManager {

  private final ListenerList<TimeTrackingUpdateListener> listeners = ListenerList.create();

  private static final TimeTrackingManager INSTANCE = new TimeTrackingManager();
  private static final DataSetListener DATASET_LISTENER_ADAPTER = new DataSetListenerAdapter(__ -> INSTANCE.trackChangeNow());

  private long committedSeconds; // = 0L

  private Long firstUncommittedChangeTimestamp; // = null
  private Long lastUncommittedChangeTimestamp; // = null

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
   * Usually you should use {@link #trackChangeNow()} instead. This method is mainly visible
   * for easier testing with arbitrary timestamps.
   *
   * <p>
   * <strong>Note:</strong> We expect to receive the calls to this method in order.
   * If an instant A is registered later than instant B (where B has a higher unix timestamp than A),
   * then an error is logged. It's handled gracefully, but should be avoided.
   * This should really only happen if the user manually set their clock to an earlier time.
   * </p>
   *
   * @param instant the timestamp when the change occurred. The seconds of that instant are recorded.
   */
  @VisibleForTesting
  void trackChangeAt(final Instant instant) {
    synchronized (INSTANCE) {
      final int tolerance = Math.max(0, RovasProperties.INACTIVITY_TOLERANCE.get());
      final Long firstTimestamp = firstUncommittedChangeTimestamp;
      final Long lastTimestamp = lastUncommittedChangeTimestamp;
      final long currentTimestamp = instant.getEpochSecond();

      Logging.debug("[TTM] {0,number,#} seconds committed, {1,number,#} - {2,number,#} uncommitted, new change at {3,number,#}", this.committedSeconds, firstTimestamp, lastTimestamp, currentTimestamp);

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
          resetAutomaticTracker(currentTimestamp, true);
        } else {
          // if inside tolerance, extend the current uncommitted timespan
          lastUncommittedChangeTimestamp = Math.max(lastTimestamp, currentTimestamp);
          fireTimeTrackingUpdateListeners();
        }
      }
    }
  }

  public void setCurrentlyTrackedSeconds(final int numSeconds) {
    synchronized (INSTANCE) {
      resetAutomaticTracker(null, false);
      this.committedSeconds = numSeconds;
      fireTimeTrackingUpdateListeners();
    }
  }

  public long commit() {
    synchronized (INSTANCE) {
      return resetAutomaticTracker(null, true);
    }
  }

  /**
   * @param newValue the new value that {@link #firstUncommittedChangeTimestamp} and
   *   {@link #lastUncommittedChangeTimestamp} will be set to
   * @param commitUncommittedTime if {@code true}, any uncommitted time will be added to the {@link #committedSeconds}.
   *   If {@code false}, any uncommitted time will be discarded.
   * @return the current number of tracked seconds, after the reset
   */
  private long resetAutomaticTracker(final Long newValue, final boolean commitUncommittedTime) {
    if (commitUncommittedTime) {
      final Long firstUncommitted = this.firstUncommittedChangeTimestamp;
      final Long lastUncommitted = this.lastUncommittedChangeTimestamp;
      if (firstUncommitted == null || lastUncommitted == null) {
        Logging.debug("[TTM] Tried to commit time, but fields were null (firstUncommitted={0},lastUncommitted={1})", firstUncommitted, lastUncommitted);
      } else {
        LoggingUtil.logIfEnabled(
          () -> MessageFormat.format(
          "[TTM] Committing uncommitted time of {0,number,#} seconds ({1,number,#} – {2,number,#}) with {3,number,#} seconds tolerance",
          lastUncommitted - firstUncommitted,
          firstUncommitted,
          lastUncommitted,
          RovasProperties.INACTIVITY_TOLERANCE.get()
          ),
          () -> null,
          Logging.LEVEL_DEBUG
        );
        this.committedSeconds +=
          Math.max(0, lastUncommitted - firstUncommitted) + // uncommitted time
          Math.min(
            Math.max(0, Instant.now().getEpochSecond() - lastUncommitted), // time since last uncommitted timestamp
            Math.max(0, RovasProperties.INACTIVITY_TOLERANCE.get()) // tolerance
          );
      }
    }
    Logging.debug("[TTM] Reset to `{0,number,#}`", newValue);
    this.firstUncommittedChangeTimestamp = newValue;
    this.lastUncommittedChangeTimestamp = newValue;
    fireTimeTrackingUpdateListeners();
    return this.committedSeconds;
  }

  /**
   * This listener notifies the (singleton) {@link TimeTrackingManager} of all changes in any {@link OsmDataLayer}.
   * Add this to JOSM's {@link LayerManager} using
   * {@link LayerManager#addAndFireLayerChangeListener(LayerManager.LayerChangeListener)}.
   */
  public static class AnyOsmDataChangeListener implements LayerManager.LayerChangeListener {
    @Override
    public void layerAdded(final LayerManager.LayerAddEvent e) {
      INSTANCE.trackChangeNow();
      Utils.instanceOfAndCast(e.getAddedLayer(), OsmDataLayer.class)
        .ifPresent(layer -> layer.data.addDataSetListener(DATASET_LISTENER_ADAPTER));
    }
    @Override
    public void layerRemoving(final LayerManager.LayerRemoveEvent e) {
      Utils.instanceOfAndCast(e.getRemovedLayer(), OsmDataLayer.class)
        .ifPresent(layer -> layer.data.removeDataSetListener(DATASET_LISTENER_ADAPTER));
    }
    @Override
    public void layerOrderChanged(final LayerManager.LayerOrderChangeEvent e) {
      // do nothing
    }
  }
}
