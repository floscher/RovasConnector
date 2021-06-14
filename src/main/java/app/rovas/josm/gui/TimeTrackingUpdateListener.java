package app.rovas.josm.gui;

import app.rovas.josm.model.TimeTrackingManager;

/**
 * This listener is used to notify other components about changes in {@link TimeTrackingManager}.
 * E.g. {@link RovasDialog} implements this interface.
 */
public interface TimeTrackingUpdateListener {
  /**
   * This is called each time an update occurs
   * @param n the total number of seconds that have been tracked so far
   */
  void updateNumberOfTrackedSeconds(final long n);
}
