package app.rovas.josm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import app.rovas.josm.fixture.UtilityClassTest;
import app.rovas.josm.util.RovasProperties;
import app.rovas.josm.util.UrlProvider;

public class RovasPropertiesTest implements UtilityClassTest<RovasProperties> {

  @Test
  public void test() {
    assertEquals("https://rovas.app/node/42", UrlProvider.getInstance().node(42).toString());
    assertEquals("https://rovas.app/node/-73", UrlProvider.getInstance().node(-73).toString());
  }
}
