package app.rovas.josm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.rovas.josm.fixture.UtilityClassTest;
import app.rovas.josm.util.URIs;
import org.junit.jupiter.api.Test;

public class RovasPropertiesTest implements UtilityClassTest<RovasProperties> {

  @Test
  public void test() {
    assertEquals("https://dev.merit.world/node/42", URIs.project(42).toString());
    assertEquals("https://dev.merit.world/node/-73", URIs.project(-73).toString());
  }
}
