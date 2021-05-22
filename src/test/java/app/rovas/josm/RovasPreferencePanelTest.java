package app.rovas.josm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.openstreetmap.josm.testutils.JOSMTestRules;

import app.rovas.josm.gui.RovasPreferencePanel;

public class RovasPreferencePanelTest {

  @RegisterExtension
  public static JOSMTestRules rules = new JOSMTestRules();

  private static final String TEST_STRING = "äöüßýôťž\uD83E\uDDB8\uD83C\uDFFF\u200D♂️";

  @Test
  public void testPanel() {
    assertValueChanges(it -> it::getActiveProjectIdValue, -1,  it -> it::setActiveProjectIdValue, Integer.MIN_VALUE);
    assertValueChanges(it -> it::getActiveProjectIdValue, -1,  it -> it::setActiveProjectIdValue, -2);
    assertValueChanges(it -> it::getActiveProjectIdValue, -1,  it -> it::setActiveProjectIdValue, -1);
    assertValueChanges(it -> it::getActiveProjectIdValue, -1,  it -> it::setActiveProjectIdValue, 0);
    assertValueChanges(it -> it::getActiveProjectIdValue, -1,  it -> it::setActiveProjectIdValue, 1);
    assertValueChanges(it -> it::getActiveProjectIdValue, 2,  it -> it::setActiveProjectIdValue, 2);
    assertValueChanges(it -> it::getActiveProjectIdValue, 123456,  it -> it::setActiveProjectIdValue, 123456);
    assertValueChanges(it -> it::getActiveProjectIdValue, Integer.MAX_VALUE,  it -> it::setActiveProjectIdValue, Integer.MAX_VALUE);
  }

  @Test
  public void testApiCredentialChanges() {
    assertStringFieldValueChanges(it -> it::getApiKeyValue, it -> it::setApiKeyValue);
    assertStringFieldValueChanges(it -> it::getApiTokenValue, it -> it::setApiTokenValue);
  }

  private <T> void assertStringFieldValueChanges(
    final Function<RovasPreferencePanel, Supplier<String>> getter,
    final Function<RovasPreferencePanel, Consumer<String>> setter
  ) {
    assertValueChanges(getter, null, setter, null);
    assertValueChanges(getter, null, setter, "");
    assertValueChanges(getter, "abc", setter, " abc \t");
    assertValueChanges(getter, TEST_STRING, setter, TEST_STRING);
  }

  private <T> void assertValueChanges(final Function<RovasPreferencePanel, Supplier<T>> getter, final T expected, final Function<RovasPreferencePanel, Consumer<T>> setter, final T actual) {
    final RovasPreferencePanel panel = new RovasPreferencePanel();
    setter.apply(panel).accept(actual);
    assertEquals(expected, getter.apply(panel).get());
  }
}
