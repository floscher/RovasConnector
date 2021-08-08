// License: GPL. For details, see LICENSE file.
package app.rovas.josm.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import app.rovas.josm.fixture.UtilityClassTest;

public class JsonUtilTest implements UtilityClassTest<JsonUtil> {

  @Test
  protected void test() {
    final JsonObject o1 = Json.createObjectBuilder()
      .add("a", 123)
      .add("b", 456)
      .add("c", -789)
      .add("e", "42")
      .add("f", "-1729")
      .add("g", "X")
      .add("h", "")
      .build();
    assertEquals(Optional.of(123), JsonUtil.extractResponseCode(o1, "a"));
    assertEquals(Optional.of(456), JsonUtil.extractResponseCode(o1, "b"));
    assertEquals(Optional.of(-789), JsonUtil.extractResponseCode(o1, "c"));
    assertEquals(Optional.empty(), JsonUtil.extractResponseCode(o1, "d"));
    assertEquals(Optional.of(42), JsonUtil.extractResponseCode(o1, "e"));
    assertEquals(Optional.of(-1729), JsonUtil.extractResponseCode(o1, "f"));
    assertEquals(Optional.empty(), JsonUtil.extractResponseCode(o1, "g"));
    assertEquals(Optional.empty(), JsonUtil.extractResponseCode(o1, "h"));
  }
}
