package app.rovas.josm.fixture;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Consumer;

/**
 * This is a {@link Consumer} that will fail a unit test, if it ever accepts any value.
 * @param <T> the type of values that this consumer can accept
 */
public class NoTimeConsumer<T> implements Consumer<T> {
  @Override
  public void accept(T t) {
    fail("The consumer is expected to not accept any value (got " + t + ")!");
  }
}
