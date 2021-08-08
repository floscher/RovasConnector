// License: GPL. For details, see LICENSE file.
package app.rovas.josm.util;

import java.util.Optional;
import java.util.regex.Pattern;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

public final class JsonUtil {
  private static final Pattern INT_PATTERN = Pattern.compile("^-?[0-9]+$");

  /**
   * Extracts an integer code from a JSON object. The code can be provided as integer,
   * or it can also be provided as string (as long as it is an integer number)
   * @param json the JSON object that contains the code stored in an attribute
   * @param key the name of the attribute key where the code is stored
   * @return the code if it is present, or otherwise an empty {@link Optional}
   */
  public static Optional<Integer> extractResponseCode(final JsonObject json, final String key) {
    final JsonValue value = json.get(key);
    final Optional<Integer> result;
    if (value instanceof JsonString) {
      result = Optional.of((JsonString) value)
        .map(JsonString::getString)
        .filter(string -> INT_PATTERN.matcher(string).matches())
        .map(Integer::parseInt);
    } else if (value instanceof JsonNumber) {
      result = Optional.of((JsonNumber) value).map(JsonNumber::intValue);
    } else {
      result = Optional.empty();
    }
    return result;
  }

  private JsonUtil() {
    // private constructor to prevent instantiation
  }
}
