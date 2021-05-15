package app.rovas.josm;

/**
 * This listener is used to notify other components about changes in {@link TimeTrackingManager}.
 * E.g. {@link RovasDialog} implements this interface.
 */
public interface TimeTrackingUpdateListener {
  void updateNumberOfTrackedSeconds(final long n);
}
