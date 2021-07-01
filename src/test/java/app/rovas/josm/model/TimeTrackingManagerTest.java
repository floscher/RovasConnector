package app.rovas.josm.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openstreetmap.josm.tools.Logging;

import app.rovas.josm.gui.TimeTrackingUpdateListener;

public class TimeTrackingManagerTest {

  private TimeTrackingManager timeTrackingManager;

  @BeforeAll
  public static void before() {
    Logging.setLogLevel(Level.FINEST);
  }

  @BeforeEach
  protected void beforeEach() {
    RovasProperties.ALREADY_TRACKED_TIME.put(42L);
    timeTrackingManager = new TimeTrackingManager();
  }

  @Test
  public void testNormalTimeSeries() {
    assertChangeEventSeries(
      new long[] { 0,   0,   0,   2,   6, 36 },
      new long[] {    123, 123, 125, 129 },
      1000
    );

    timeTrackingManager.setCurrentlyTrackedSeconds(0);

    assertChangeEventSeries(
      new long[] { 0, 0, 1, 2, 3, 33, 35, 65 },
      new long[] {    1, 2, 3, 4, 60, 62 },
      1000
    );

    timeTrackingManager.setCurrentlyTrackedSeconds(0);

    assertChangeEventSeries(
      new long[] { 0, 0, 15, 45, 50, 55, 75,  90, 120 },
      new long[] {    0, 15, 60, 65, 70, 90, 105 },
      1000
    );
  }

  @Test
  public void testTimeSeriesWithBackwardsComponent() {
    Logging.clearLastErrorAndWarnings();
    assertChangeEventSeries(
      new long[] { 0,   0,   5,  12,  12,  12,  30, 60 },
      new long[] {    100, 105, 112, 106, 110, 130 },
      1000
    );
    assertBackwardsClockWarningWasFired(2);

    timeTrackingManager.setCurrentlyTrackedSeconds(0);

    assertChangeEventSeries(
      new long[] { 0,   0,   5,  12, 12,  22,  27, 57 },
      new long[] {    100, 105, 112, 90, 100, 105 },
      1000
    );
    assertBackwardsClockWarningWasFired(1);
  }

  @Test
  public void testEarlyCommit() {
    assertChangeEventSeries(
      new long[]{ 0,  0,  20,  42,  72,  77, 97, },
      new long[]{   100, 120, 142, 175, 180,  },
      200
    );
  }

  @Test
  protected void testAddPreviousTime() throws ReflectiveOperationException {
    final MockTimeListener listener = new MockTimeListener();
    timeTrackingManager.addAndFireTimeTrackingUpdateListener(listener);

    Stream.of(50, 73, 101).map(Instant::ofEpochSecond).forEach(timeTrackingManager::trackChangeAt);
    timeTrackingManager.handlePreviouslyTrackedSeconds(true);
    Stream.of(120, 125).map(Instant::ofEpochSecond).forEach(timeTrackingManager::trackChangeAt);
    timeTrackingManager.commit(Instant.ofEpochSecond(130));

    assertArrayEquals(new long[]{ 0, 0, 23, 51, 93, 112, 117, 122 }, listener.getReceivedUpdates());

    timeTrackingManager.removeTimeTrackingUpdateListener(listener);
  }

  /**
   * @param expectedUpdates the expected values that are reported by the {@link TimeTrackingManager} to the {@link TimeTrackingUpdateListener}
   * @param changeEventTimestamps the unix timestamps at which {@link TimeTrackingManager#trackChangeAt(Instant)} is called
   */
  private void assertChangeEventSeries(
    final long[] expectedUpdates,
    final long[] changeEventTimestamps,
    final long commitTimestamp
  ) {
    final MockTimeListener listener = new MockTimeListener();
    timeTrackingManager.addAndFireTimeTrackingUpdateListener(listener);

    Arrays.stream(changeEventTimestamps)
      .mapToObj(Instant::ofEpochSecond)
      .forEach(timeTrackingManager::trackChangeAt);
    Logging.info("Expectation: " + Arrays.stream(expectedUpdates).mapToObj(String::valueOf).collect(Collectors.joining(", ")));
    assertEquals(
      expectedUpdates[expectedUpdates.length - 1],
      timeTrackingManager.commit(Instant.ofEpochSecond(commitTimestamp)),
      () -> "Expected different time after commit. Received these updates: " + Arrays.stream(listener.getReceivedUpdates()).mapToObj(String::valueOf).collect(Collectors.joining(", "))
    );
    Logging.info("     Actual: " + Arrays.stream(listener.getReceivedUpdates()).mapToObj(String::valueOf).collect(Collectors.joining(", ")));
    assertArrayEquals(expectedUpdates, listener.getReceivedUpdates());

    timeTrackingManager.removeTimeTrackingUpdateListener(listener);
  }

  private void assertBackwardsClockWarningWasFired(final int n) {
    final List<String> warnings = Logging.getLastErrorAndWarnings();
    assertEquals(n, warnings.size(), () -> "Expected exactly " + n + " warning messages logged!");
    assertTrue(warnings.stream().allMatch(it -> it.contains(TimeTrackingManager.LOG_MESSAGE_BACKWARDS_CLOCK)));
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
