package app.rovas.josm.util;

import java.util.function.Supplier;
import java.util.logging.Level;

import org.openstreetmap.josm.tools.Logging;

public final class LoggingUtil {
  private LoggingUtil() {
    // private constructor to prevent instantiation
  }

  @SuppressWarnings("PMD.GuardLogStatement")
  public static void logIfEnabled(final Supplier<String> messageSupplier, final Supplier<Throwable> throwableSupplier, final Level level) {
    if (Logging.isLoggingEnabled(level)) {
      Logging.log(level, messageSupplier.get(), throwableSupplier.get());
    }
  }
}
