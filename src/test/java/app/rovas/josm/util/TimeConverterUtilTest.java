package app.rovas.josm.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import app.rovas.josm.fixture.UtilityClassTest;

public class TimeConverterUtilTest implements UtilityClassTest<TimeConverterUtil> {

  @Test
  public void secondsToMinutes() {
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
    assertEquals(153_722_867_280_912_930L, TimeConverterUtil.secondsToMinutes(Long.MAX_VALUE));
  }

  @Test
  public void minutesToFractionalChrons() {
    assertEquals("0", TimeConverterUtil.minutesToFractionalChrons(Integer.MIN_VALUE));
    assertEquals("0", TimeConverterUtil.minutesToFractionalChrons(0));
    assertEquals("⅙", TimeConverterUtil.minutesToFractionalChrons(1));
    assertEquals("⅓", TimeConverterUtil.minutesToFractionalChrons(2));
    assertEquals("½", TimeConverterUtil.minutesToFractionalChrons(3));
    assertEquals("⅔", TimeConverterUtil.minutesToFractionalChrons(4));
    assertEquals("⅚", TimeConverterUtil.minutesToFractionalChrons(5));
    assertEquals("1", TimeConverterUtil.minutesToFractionalChrons(6));
    assertEquals("5 ⅚", TimeConverterUtil.minutesToFractionalChrons(35));
    assertEquals("7", TimeConverterUtil.minutesToFractionalChrons(42));
    assertEquals("10", TimeConverterUtil.minutesToFractionalChrons(60));
    assertEquals("20", TimeConverterUtil.minutesToFractionalChrons(120));
    assertEquals("357913941 ⅙", TimeConverterUtil.minutesToFractionalChrons(Integer.MAX_VALUE));
  }
}
