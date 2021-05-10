package app.rovas.josm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

public class RovasPreferencePanelTest {
  private static final String TEST_STRING = "äöüßýôťž\uD83E\uDDB8\uD83C\uDFFF\u200D♂️";

  @Test
  public void testPanel() {
    assertValueChanges(it -> it::getActiveProjectIdValue, -1,  it -> it::setActiveProjectIdValue, Integer.MIN_VALUE);
    assertValueChanges(it -> it::getActiveProjectIdValue, -1,  it -> it::setActiveProjectIdValue, -2);
    assertValueChanges(it -> it::getActiveProjectIdValue, -1,  it -> it::setActiveProjectIdValue, -1);
    assertValueChanges(it -> it::getActiveProjectIdValue, -1,  it -> it::setActiveProjectIdValue, 0);
    assertValueChanges(it -> it::getActiveProjectIdValue, 1,  it -> it::setActiveProjectIdValue, 1);
    assertValueChanges(it -> it::getActiveProjectIdValue, 123456,  it -> it::setActiveProjectIdValue, 123456);
    assertValueChanges(it -> it::getActiveProjectIdValue, Integer.MAX_VALUE,  it -> it::setActiveProjectIdValue, Integer.MAX_VALUE);
  }

  @Test
  public void testApiKeyChanges() {
    assertValueChanges(it -> it::getApiKeyValue, null, it -> it::setApiKeyValue, null);
    assertValueChanges(it -> it::getApiKeyValue, null, it -> it::setApiKeyValue, "");
    assertValueChanges(it -> it::getApiKeyValue, "abc", it -> it::setApiKeyValue, " abc \t");
    assertValueChanges(it -> it::getApiKeyValue, TEST_STRING, it -> it::setApiKeyValue, TEST_STRING);

    assertValueChanges(it -> it::getApiTokenValue, null, it -> it::setApiTokenValue, null);
    assertValueChanges(it -> it::getApiTokenValue, null, it -> it::setApiTokenValue, "");
    assertValueChanges(it -> it::getApiTokenValue, "abc", it -> it::setApiTokenValue, " abc \t");
    assertValueChanges(it -> it::getApiTokenValue, TEST_STRING, it -> it::setApiTokenValue, TEST_STRING);
  }

  private <T> void assertValueChanges(final Function<RovasPreferencePanel, Supplier<T>> getter, final T expected, final Function<RovasPreferencePanel, Consumer<T>> setter, final T actual) {
    final RovasPreferencePanel panel = new RovasPreferencePanel();
    setter.apply(panel).accept(actual);
    assertEquals(expected, getter.apply(panel).get());
  }
}
