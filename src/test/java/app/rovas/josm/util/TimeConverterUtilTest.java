package app.rovas.josm.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.openstreetmap.josm.tools.Logging;

import app.rovas.josm.fixture.UtilityClassTest;

public class TimeConverterUtilTest implements UtilityClassTest<TimeConverterUtil> {

  @Test
  protected void secondsToMinutes() {
    assertEquals(0, TimeConverterUtil.secondsToMinutes(Long.MIN_VALUE));
    assertEquals(0, TimeConverterUtil.secondsToMinutes(0));
    assertEquals(0, TimeConverterUtil.secondsToMinutes(29));
    assertEquals(1, TimeConverterUtil.secondsToMinutes(30));
    assertEquals(1, TimeConverterUtil.secondsToMinutes(89));
    assertEquals(2, TimeConverterUtil.secondsToMinutes(90));
    assertEquals(2, TimeConverterUtil.secondsToMinutes(100));
    assertEquals(17, TimeConverterUtil.secondsToMinutes(1000));
    assertEquals(29, TimeConverterUtil.secondsToMinutes(1729));
    assertEquals(194, TimeConverterUtil.secondsToMinutes(11669));
    assertEquals(195, TimeConverterUtil.secondsToMinutes(11670));
    assertEquals(195, TimeConverterUtil.secondsToMinutes(11682));

    assertEquals(TimeConverterUtil.MAX_MINUTES - 1, TimeConverterUtil.secondsToMinutes(TimeConverterUtil.MAX_SECONDS - 61));
    assertEquals(TimeConverterUtil.MAX_MINUTES - 1, TimeConverterUtil.secondsToMinutes(TimeConverterUtil.MAX_SECONDS - 60));
    assertEquals(TimeConverterUtil.MAX_MINUTES, TimeConverterUtil.secondsToMinutes(TimeConverterUtil.MAX_SECONDS - 59));
    assertEquals(TimeConverterUtil.MAX_MINUTES, TimeConverterUtil.secondsToMinutes(TimeConverterUtil.MAX_SECONDS - 1));
    assertEquals(TimeConverterUtil.MAX_MINUTES, TimeConverterUtil.secondsToMinutes(TimeConverterUtil.MAX_SECONDS));
    assertEquals(TimeConverterUtil.MAX_MINUTES, TimeConverterUtil.secondsToMinutes(Long.MAX_VALUE));
  }

  @Test
  protected void minutesToFractionalChrons() {
    assertMinutesToChrons("0", 0, Integer.MIN_VALUE);
    assertMinutesToChrons("0", 0, 0);
    assertMinutesToChrons("⅙", 1 / 6.0, 1);
    assertMinutesToChrons("⅓", 1 / 3.0, 2);
    assertMinutesToChrons("½", 1 / 2.0, 3);
    assertMinutesToChrons("⅔", 2 / 3.0, 4);
    assertMinutesToChrons("⅚", 5 / 6.0, 5);
    assertMinutesToChrons("1", 1, 6);
    assertMinutesToChrons("5 ⅚", 5 + 5 / 6.0, 35);
    assertMinutesToChrons("7", 7, 42);
    assertMinutesToChrons("10", 10, 60);
    assertMinutesToChrons("20", 20, 120);
    assertMinutesToChrons("288 ⅙", 288 + 1 / 6.0, 1729);

    assertMinutesToChrons("357913939 ⅔", 357913939 + 4 / 6.0, TimeConverterUtil.MAX_MINUTES - 1);
    assertMinutesToChrons("357913939 ⅚", 357913939 + 5 / 6.0, TimeConverterUtil.MAX_MINUTES);
    assertMinutesToChrons("357913939 ⅚", 357913939 + 5 / 6.0, Integer.MAX_VALUE);
  }

  private void assertMinutesToChrons(final String expectedFraction, final double expectedDecimal, final int actualMinutes) {
    Logging.info("Expecting that {0,number,#} minutes are converted to {1} chrons = {2,number,#.########} chrons", actualMinutes, expectedFraction, expectedDecimal);
    assertEquals(expectedFraction, TimeConverterUtil.minutesToFractionalChrons(actualMinutes));
    final double actualDecimal = TimeConverterUtil.minutesToChrons(actualMinutes);
    assertEquals(expectedDecimal, actualDecimal, 1e-9, () -> String.format(Locale.ROOT, "Expected %f\n  actual %f\n", expectedDecimal, actualDecimal));
  }
}
