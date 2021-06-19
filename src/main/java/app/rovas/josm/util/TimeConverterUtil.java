// License: GPL. For details, see LICENSE file.
package app.rovas.josm.util;

import org.openstreetmap.josm.tools.Utils;

/**
 * Utility class that contains a bunch of tools to convert time in between hours, minutes, seconds and the currency chrons.
 */
public final class TimeConverterUtil {
  /**
   * <p>The maximum number of hours that we track, which is one less than the maximum number of hours that could be
   * converted to minutes and still be an integer. That way we could track this many hours plus 59 minutes maximum.</p>
   * <p>That's {@code 35,791,393} hours (~4083 years).</p>
   */
  public static final int MAX_HOURS = Integer.MAX_VALUE / 60 - 1;
  /**
   * <p>The maximum amount of minutes that we track. This is 59 Minutes plus the maximum multiple of 60 minutes,
   * so the result still is below {@link Integer#MAX_VALUE}.</p>
   * <p>That's {@code 2,147,483,639} minutes (~4083 years)</p>
   */
  public static final int MAX_MINUTES = MAX_HOURS * 60 + 59;
  /**
   * The maximum number of seconds that we track ({@link #MAX_MINUTES} and 29 seconds).
   * That's the highest number of seconds that rounds to {@link #MAX_MINUTES} minutes when converting
   * to minutes ({@link #secondsToMinutes(long)}).
   */
  public static final long MAX_SECONDS = MAX_MINUTES * 60L + 29;

  private TimeConverterUtil() {
    // private constructor to prevent instantiation
  }

  /**
   * Clamps the amount of seconds to the range 0..{@link #MAX_SECONDS}.
   * Basically equivalent to {@link Utils#clamp(int, int, int)}, but for a {@link Long} value and this specific range.
   * @param seconds the original amount
   * @return the clamped amount
   */
  public static long clampToSeconds(final long seconds) {
    return Math.max(0, Math.min(MAX_SECONDS, seconds));
  }

  /**
   * Converts seconds to minutes, this method rounds to the nearest full minute (rounds 29s or less down, 30s or more up).
   * The result will always be non-negative. For negative parameter values, this method always returns 0.
   * @param seconds the number of seconds that should be converted to minutes
   * @return the number of minutes that corresponds to the given number of seconds
   */
  public static int secondsToMinutes(final long seconds) {
    // the 30 seconds are added, so 30 or more seconds within a minute are rounded up to the next full minute
    return (int) ((clampToSeconds(seconds) + 30L) / 60);
  }

  /**
   * Converts a number of minutes to the equivalent amount in chrons.
   * Values are clamped to the range of 0..{@link #MAX_MINUTES}, then divided by {@code 6.0}.
   * @param minutes the number of minutes to be converted
   * @return the equivalent amount in chrons
   */
  public static double minutesToChrons(final int minutes) {
    return Utils.clamp(minutes, 0, MAX_MINUTES) / 6.0;
  }
}
