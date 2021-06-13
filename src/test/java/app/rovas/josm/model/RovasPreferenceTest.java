package app.rovas.josm.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class RovasPreferenceTest {

  @Test
  protected void test() {
    final RovasPreference pref = new RovasPreference();

    pref.getPrefPanel().setInactivityTolerance(42);
    pref.getPrefPanel().setApiKeyValue("key");
    pref.getPrefPanel().setApiTokenValue("token");
    pref.getPrefPanel().setActiveProjectIdValue(1234);

    assertEquals(RovasProperties.INACTIVITY_TOLERANCE.getDefaultValue(), RovasProperties.INACTIVITY_TOLERANCE.get());
    assertNull(RovasProperties.ROVAS_API_KEY.get());
    assertNull(RovasProperties.ROVAS_API_TOKEN.get());
    assertEquals(RovasProperties.ACTIVE_PROJECT_ID.getDefaultValue(), RovasProperties.ACTIVE_PROJECT_ID.get());

    pref.ok();

    assertEquals(42, RovasProperties.INACTIVITY_TOLERANCE.get());
    assertEquals("key", RovasProperties.ROVAS_API_KEY.get());
    assertEquals("token", RovasProperties.ROVAS_API_TOKEN.get());
    assertEquals(1234, RovasProperties.ACTIVE_PROJECT_ID.get());
  }
}
