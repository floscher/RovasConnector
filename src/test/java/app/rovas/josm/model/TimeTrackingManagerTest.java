package app.rovas.josm.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openstreetmap.josm.tools.Logging;

import app.rovas.josm.gui.TimeTrackingUpdateListener;

public class TimeTrackingManagerTest {

  @Test
  public void testNormalTimeSeries() {
    assertChangeEventSeries(
      new long[] { 0,   0,   0,   2,   6, 36 },
      new long[] {    123, 123, 125, 129 }
    );

    assertChangeEventSeries(
      new long[] { 0, 0, 1, 2, 3, 33, 35, 65 },
      new long[] {    1, 2, 3, 4, 60, 62 }
    );

    assertChangeEventSeries(
      new long[] { 0, 0, 15, 45, 50, 55, 75,  90, 120 },
      new long[] {    0, 15, 60, 65, 70, 90, 105 }
    );
  }

  @Test
  public void testTimeSeriesWithBackwardsComponent() {
    Logging.clearLastErrorAndWarnings();
    assertChangeEventSeries(
      new long[] { 0,   0,   5,  12,  12,  30, 60 },
      new long[] {    100, 105, 112, 106, 130 }
    );
    assertBackwardsClockWarningWasFired();

    assertChangeEventSeries(
      new long[] { 0,   0,   5,  12, 42,  52,  57, 87 },
      new long[] {    100, 105, 112, 90, 100, 105 }
    );
    assertBackwardsClockWarningWasFired();
  }

  private void assertChangeEventSeries(
    final long[] expectedUpdates,
    final long[] changeEventTimestamps
  ) {
    final MockTimeListener listener = new MockTimeListener();
    TimeTrackingManager.getInstance().addAndFireTimeTrackingUpdateListener(listener);

    Arrays.stream(changeEventTimestamps)
      .mapToObj(Instant::ofEpochSecond)
      .forEach(TimeTrackingManager.getInstance()::trackChangeAt);
    assertEquals(expectedUpdates[expectedUpdates.length - 1], TimeTrackingManager.getInstance().commit());
    assertArrayEquals(expectedUpdates, listener.getReceivedUpdates());

    // Reset time tracking manager
    TimeTrackingManager.getInstance().setCurrentlyTrackedSeconds(0);
    TimeTrackingManager.getInstance().removeTimeTrackingUpdateListener(listener);
  }

  private void assertBackwardsClockWarningWasFired() {
    final List<String> warnings = Logging.getLastErrorAndWarnings();
    assertEquals(1, warnings.size());
    assertTrue(warnings.get(0).contains("Your clock seems to have been running backwards!"));
    Logging.clearLastErrorAndWarnings();
  }

  private static class MockTimeListener implements TimeTrackingUpdateListener {
    private final Collection<Long> actualUpdates = new ArrayList<>();

    @Override
    public void updateNumberOfTrackedSeconds(long n) {
      actualUpdates.add(n);
    }

    public long[] getReceivedUpdates() {
      return actualUpdates.stream().mapToLong(it -> it).toArray();
    }
  }
}
