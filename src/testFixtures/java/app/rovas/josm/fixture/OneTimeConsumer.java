package app.rovas.josm.fixture;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * A {@link Consumer} that will accept exactly one not-null value.
 * You can specify what the expected value should be and if the actual value isn't equal, the unit test will fail.
 * @param <T> the type of value that this consumer can accept
 */
public class OneTimeConsumer<T> implements Consumer<T> {
  private final T expected;
  private final BiPredicate<T, T> equals;
  private T actual;

  /**
   * @param expected the expected value, will be compared to the actual value using {@link Objects#equals(Object, Object)}.
   */
  public OneTimeConsumer(final T expected) {
    this(expected, Objects::equals);
  }

  /**
   * @param expected the value that we expect to accept
   * @param equals a custom {@link BiPredicate} that will be used to determine if expected and actual value are equal
   */
  public OneTimeConsumer(final T expected, final BiPredicate<T, T> equals) {
    this.expected = Objects.requireNonNull(expected);
    this.equals = equals;
  }

  @Override
  public void accept(T t) {
    assertNotNull(t, "Consumer does not accept `null`!");
    assertNull(actual, "Consumer must only accept one value!");
    this.actual = t;
    System.out.println("Accepted: " + t);
  }

  public void assertHasAccepted() {
    assertNotNull(actual, "Consumer is expected to accept a value, but did not accept so far!");
    assertTrue(equals.test(expected, actual), "Consumer hasn't accepted the expected value!\n\texpected: " + expected + "\n\t  actual: " + actual);
  }
}
