package app.rovas.josm.fixture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Adds a test method, which checks that coding standards for utility classes are followed.
 * @param <T> the class under test
 */
public interface UtilityClassTest<T> {

  /**
   * Tests utility classes for common coding standards (exactly one constructor that's private,
   * only static methods, …) and fails if one of those standards is not met.
   * This is inspired by <a href="https://stackoverflow.com/a/10872497">an answer on StackOverflow.com</a> .
   */
  @Test
  default void testUtilityClass() throws ReflectiveOperationException {
    // Find class object for generic type T
    @SuppressWarnings("unchecked")
    final Class<T> c = (Class<T>) (
      Arrays.stream(getClass().getGenericInterfaces())
        .map(it -> it instanceof ParameterizedType ? (ParameterizedType) it : null)
        .filter(it -> it != null && UtilityClassTest.class.equals(it.getRawType()))
        .findFirst()
        .orElseThrow(RuntimeException::new)
        .getActualTypeArguments()[0]
    );

    // class must be final
    assertTrue(Modifier.isFinal(c.getModifiers()));
    // with exactly one constructor
    assertEquals(1, c.getDeclaredConstructors().length);
    final Constructor<?> constructor = c.getDeclaredConstructors()[0];
    // constructor has to be private
    assertTrue(Modifier.isPrivate(constructor.getModifiers()));

    // Call private constructor for code coverage
    constructor.setAccessible(true);
    constructor.newInstance();
    constructor.setAccessible(false);

    for (Method m : c.getMethods()) {
      // Check if all methods are static
      assertTrue(m.getDeclaringClass() != c || Modifier.isStatic(m.getModifiers()));
    }
  }
}
