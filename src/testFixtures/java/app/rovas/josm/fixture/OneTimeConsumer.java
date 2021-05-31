package app.rovas.josm.fixture;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

public class OneTimeConsumer<T> implements Consumer<T> {
  private final T expected;
  private final BiPredicate<T, T> equals;
  private final Function<T, String> toString;
  private T actual;

  public OneTimeConsumer(final T expected) {
    this(expected, Objects::equals, T::toString);
  }

  public OneTimeConsumer(final T expected, final BiPredicate<T, T> equals, final Function<T, String> toString) {
    this.expected = Objects.requireNonNull(expected);
    this.equals = equals;
    this.toString = toString;
  }

  @Override
  public void accept(T t) {
    assertNotNull(t, "Consumer does not accept `null`!");
    assertNull(actual, "Consumer must only accept one value!");
    this.actual = t;
    System.out.println("Accepted: " + toString.apply(t));
  }

  public void assertHasAccepted() {
    assertNotNull(actual, "Consumer is expected to accept a value, but did not accept so far!");
    assertTrue(equals.test(expected, actual), "Consumer hasn't accepted the expected value!\n\texpected: " + toString.apply(expected) + "\n\t  actual: " + toString.apply(actual));
  }
}
