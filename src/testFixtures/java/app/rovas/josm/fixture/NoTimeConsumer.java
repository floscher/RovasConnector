package app.rovas.josm.fixture;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Consumer;

public class NoTimeConsumer<T> implements Consumer<T> {
  @Override
  public void accept(T t) {
    fail("The consumer is expected to not accept any value!");
  }
}
