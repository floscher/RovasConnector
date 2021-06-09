package app.rovas.josm.util;

public final class TimeConverterUtil {
  /**
   * The maximum number of hours, so that this number of hours plus 59 minutes is still an integer
   */
  public static final int MAX_HOURS = Integer.MAX_VALUE / 60 - 1;
  /**
   * The maximum number of seconds that we can track ({@link #MAX_HOURS} hours and 59 minutes).
   */
  public static final long MAX_SECONDS = (MAX_HOURS * 60L + 59L) * 60L;

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
  public static long secondsToMinutes(final long seconds) {
    if (seconds < 30) {
      return 0L;
    }
    // the 30 seconds are subtracted, so 30 or more seconds within a minute are rounded up to the next full minute
    return (seconds - 30) / 60 + 1;
  }

  public static String minutesToFractionalChrons(final int minutes) {
    if (minutes <= 0) {
      return "0";
    }
    final String fraction;
    switch (minutes % 6) {
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
    final int whole = (minutes / 6);
    return (whole != 0 || fraction.isEmpty() ? whole : "") +
      (fraction.isEmpty() || whole == 0 ? fraction : " " + fraction);
  }
}
