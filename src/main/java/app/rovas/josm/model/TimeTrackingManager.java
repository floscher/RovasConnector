// License: GPL. For details, see LICENSE file.
package app.rovas.josm.model;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.gui.layer.LayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

import app.rovas.josm.gui.TimeTrackingUpdateListener;
import app.rovas.josm.util.TimeConverterUtil;
import app.rovas.josm.util.VisibleForTesting;

/**
 * <p>Tracks changes and calculates the amount of time recorded as work time.</p>
 *
 * <p>A point in time is considered work time, if it is within {@link RovasProperties#INACTIVITY_TOLERANCE} seconds
 * of a tracked change.</p>
 *
 * A change can be tracked via {@link #trackChangeNow()}.
 * When you want to retrieve the number of tracked seconds, call {@link #commit()}.
 * In order to reset the currently tracked time, call {@link #setCurrentlyTrackedSeconds(long)}
 */
public final class TimeTrackingManager {
  @VisibleForTesting
  static final String LOG_MESSAGE_BACKWARDS_CLOCK = "Your clock seems to have been running backwards!";

  private final ListenerList<TimeTrackingUpdateListener> listeners = ListenerList.create();

  private static final TimeTrackingManager INSTANCE = new TimeTrackingManager();
  private static final DataSetListener DATASET_LISTENER_ADAPTER = new DataSetListenerAdapter(__ -> INSTANCE.trackChangeNow());

  /**
   * Saves the initial value when the time tracking manager is initialized
   */
  private final long previouslyTrackedSeconds = RovasProperties.ALREADY_TRACKED_TIME.get();
  private final AtomicBoolean previouslyTrackedTimeIsAlreadyAdded = new AtomicBoolean(false);

  /**
   * The number of seconds that are already committed.
   */
  private long committedSeconds; // = 0L

  /**
   * The timestamp when the first change occured that was not counted towards the {@link #committedSeconds}.
   * If there are currently no uncommitted changes, this is set to {@code null}.
   */
  private Long firstUncommittedChangeTimestamp; // = null
  /**
   * The timestamp when the last change occured that was not counted towards the {@link #committedSeconds}.
   * If there are currently no uncommitted changes, this is set to {@code null}.
   */
  private Long lastUncommittedChangeTimestamp; // = null

  private TimeTrackingManager() {
    // private constructor to avoid instantiation
  }

  /**
   * Adds a listener that will be notified once immediately and then also any time the amount of tracked seconds changes.
   * @param listener the listener that will be notified
   */
  public void addAndFireTimeTrackingUpdateListener(final TimeTrackingUpdateListener listener) {
    listeners.addListener(listener);
    fireTimeTrackingUpdateListeners();
  }

  /**
   * <p>Adds or discards the number of seconds saved in {@link RovasProperties#ALREADY_TRACKED_TIME}.
   * This is used to keep tracked time across restarts of JOSM.</p>
   *
   * <p>This will not interfere with any uncommitted time, but instead the time is immediately committed.
   * Also, the property {@link RovasProperties#ALREADY_TRACKED_TIME} is set to {@code 0}.</p>
   *
   * @param shouldBeAdded if this is {@code true}, the previously tracked time is added, otherwise it is discarded
   *   without changing the currently tracked time.
   */
  public void handlePreviouslyTrackedSeconds(final boolean shouldBeAdded) {
    synchronized (INSTANCE) {
      if (!previouslyTrackedTimeIsAlreadyAdded.get()) {
        previouslyTrackedTimeIsAlreadyAdded.set(true);
        RovasProperties.ALREADY_TRACKED_TIME.put(0L);
        if (shouldBeAdded) {
          try {
            committedSeconds = Math.addExact(committedSeconds, previouslyTrackedSeconds);
          } catch (ArithmeticException e) {
            committedSeconds = TimeConverterUtil.MAX_SECONDS;
          }
          fireTimeTrackingUpdateListeners();
        }
      }
    }
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

  /**
   * @return the amount of previously tracked time that could be added to the currently tracked time
   */
  public long getPreviouslyTrackedSeconds() {
    synchronized (INSTANCE) {
      return
        previouslyTrackedTimeIsAlreadyAdded.get() || TimeConverterUtil.secondsToMinutes(previouslyTrackedSeconds) <= 0
          ? 0
          : previouslyTrackedSeconds;
    }
  }

  /**
   * Removes the listener, it won't receive any updates anymore about changes of the tracked time.
   * @param listener the listener to be removed
   */
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
        resetAutomaticTracker(currentTimestamp, Optional.empty());
      } else if (lastTimestamp != null) {
        // something went wrong, current time is before the last timestamp that was recorded previously.
        if (currentTimestamp < lastTimestamp) {
          Logging.error(LOG_MESSAGE_BACKWARDS_CLOCK);
        }
        // In case the current time is not inside the uncommitted timespan (including tolerance), then that timespan is committed and a new timespan is started.
        if (currentTimestamp > lastTimestamp + tolerance || currentTimestamp < firstTimestamp) {
          resetAutomaticTracker(currentTimestamp, Optional.of(instant));
        } else {
          // if inside tolerance, extend the current uncommitted timespan
          lastUncommittedChangeTimestamp = Math.max(lastTimestamp, currentTimestamp);
          fireTimeTrackingUpdateListeners();
        }
      }
    }
  }

  /**
   * Sets the {@link #committedSeconds} to the given number of seconds.
   * The uncommitted time is reset.
   * @param numSeconds the number of seconds that should be set as {@link #committedSeconds}
   */
  public void setCurrentlyTrackedSeconds(final long numSeconds) {
    synchronized (INSTANCE) {
      resetAutomaticTracker(null, Optional.empty());
      this.committedSeconds = TimeConverterUtil.clampToSeconds(numSeconds);
      fireTimeTrackingUpdateListeners();
    }
  }

  /**
   * <p>Finalizes the tracked time until the current point in time. This is needed, because
   * {@link RovasProperties#INACTIVITY_TOLERANCE} seconds after each tracked change are also counted as work time.</p>
   *
   * <p>Note that if this is called within {@link RovasProperties#INACTIVITY_TOLERANCE} seconds of a change,
   * not the full {@link RovasProperties#INACTIVITY_TOLERANCE} will be committed, but only the part from the change
   * until the call to this method.</p>
   *
   * @return the current number of tracked seconds, after the commit happened
   */
  public long commit() {
    return commit(Instant.now());
  }

  @VisibleForTesting
  long commit(final Instant instant) {
    synchronized (INSTANCE) {
      return resetAutomaticTracker(null, Optional.of(instant));
    }
  }

  /**
   * @param newValue the new value that {@link #firstUncommittedChangeTimestamp} and
   *   {@link #lastUncommittedChangeTimestamp} will be set to
   * @param commitTimeUntil if an {@link Instant} is present, any uncommitted time will be added to the
   *   {@link #committedSeconds}. If no {@link Instant} is present, any uncommitted time will be discarded.
   * @return the current number of tracked seconds, after the reset
   */
  private long resetAutomaticTracker(final Long newValue, final Optional<Instant> commitTimeUntil) {
    commitTimeUntil.ifPresent(currentInstant -> {
      final Long firstUncommitted = this.firstUncommittedChangeTimestamp;
      final Long lastUncommitted = this.lastUncommittedChangeTimestamp;
      if (firstUncommitted == null || lastUncommitted == null) {
        Logging.debug("[TTM] Tried to commit time, but fields were null (firstUncommitted={0},lastUncommitted={1})", firstUncommitted, lastUncommitted);
      } else {
        if (Logging.isDebugEnabled()) {
          Logging.debug(MessageFormat.format(
            "[TTM] Committing uncommitted time of {0,number,#} seconds ({1,number,#} – {2,number,#}) with {3,number,#} seconds tolerance",
            lastUncommitted - firstUncommitted,
            firstUncommitted,
            lastUncommitted,
            RovasProperties.INACTIVITY_TOLERANCE.get()
          ));
        }
        this.committedSeconds = TimeConverterUtil.clampToSeconds(
          this.committedSeconds +
          Math.max(0, lastUncommitted - firstUncommitted) + // uncommitted time
          Math.min(
            Math.max(0, currentInstant.getEpochSecond() - lastUncommitted), // time since last uncommitted timestamp
            Math.max(0, RovasProperties.INACTIVITY_TOLERANCE.get()) // tolerance
          )
        );
      }
    });
    Logging.debug("[TTM] Reset to `{0,number,#}` ({1,number,#} committed)", newValue, committedSeconds);
    this.firstUncommittedChangeTimestamp = newValue;
    this.lastUncommittedChangeTimestamp = newValue;
    fireTimeTrackingUpdateListeners();
    return this.committedSeconds;
  }

  /**
   * <p>This listener notifies the (singleton) {@link TimeTrackingManager} of all changes in any {@link OsmDataLayer}
   * that are relevant for time tracking.</p>
   * <p>Add this to JOSM's {@link LayerManager} using
   * {@link LayerManager#addAndFireLayerChangeListener(LayerManager.LayerChangeListener)}.</p>
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
