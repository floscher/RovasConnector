package app.rovas.josm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import app.rovas.josm.fixture.UtilityClassTest;
import app.rovas.josm.util.URIs;

public class RovasPropertiesTest implements UtilityClassTest<RovasProperties> {

  @Test
  public void test() {
    assertEquals("https://dev.merit.world/node/42", URIs.project(42).toString());
    assertEquals("https://dev.merit.world/node/-73", URIs.project(-73).toString());

    TimeTrackingManager.getInstance().trackChangeAt(Instant.now());
  }
}
