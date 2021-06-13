package app.rovas.josm.util;

import org.openstreetmap.josm.tools.Utils;

public final class TimeConverterUtil {
  /**
   * The maximum number of hours, so that this number of hours plus 59 minutes is still an integer
   */
  public static final int MAX_HOURS = Integer.MAX_VALUE / 60 - 1;
  public static final int MAX_MINUTES = MAX_HOURS * 60 + 59;
  /**
   * The maximum number of seconds that we track ({@link #MAX_HOURS} hours, 59 minutes and 29 seconds).
   * That's the highest number of seconds that round to {@link #MAX_MINUTES} minutes.
   */
  public static final long MAX_SECONDS = MAX_MINUTES * 60L + 29;

  private TimeConverterUtil() {
    // private constructor to prevent instantiation
  }

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

  public static double minutesToChrons(final int minutes) {
    return Utils.clamp(minutes, 0, MAX_MINUTES) / 6.0;
  }

  public static String minutesToFractionalChrons(final int minutes) {
    final int realMinutes = Utils.clamp(minutes, 0, MAX_MINUTES);
    final String fraction;
    switch (realMinutes % 6) {
      case 1:
        fraction = "⅙";
        break;
      case 2:
        fraction = "⅓";
        break;
      case 3:
        fraction = "½";
        break;
      case 4:
        fraction = "⅔";
        break;
      case 5:
        fraction = "⅚";
        break;
      default:
        fraction = "";
    }
    final int whole = (realMinutes / 6);
    return (whole != 0 || fraction.isEmpty() ? whole : "") +
      (fraction.isEmpty() || whole == 0 ? fraction : " " + fraction);
  }
}
