package app.rovas.josm.util;

import java.util.Optional;
import java.util.function.Predicate;

import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;

import org.openstreetmap.josm.data.preferences.AbstractProperty;
import org.openstreetmap.josm.data.preferences.AbstractToStringProperty;

/**
 * Encapsulates a {@link AbstractToStringProperty} and treats certain values as {@code null}.
 *
 * This class has a subset of the API of {@link AbstractToStringProperty}, it's intended as a drop-in replacement
 * that allows to treat certain values as {@code null}.
 *
 * @param <T> the type of the values that the property can store
 */
public class NullableProperty<T> {
  private final AbstractToStringProperty<T> internalProperty;
  private final Predicate<T> acceptableValues;

  /**
   * @param internalProperty the property that should be treated as nullable
   * @param acceptableValues values for which this predicate returns true will be persisted as-is,
   *   all other values will be treated as {@code null}. The value {@code null} will always be treated
   *   as {@code null}, it is never passed to the predicate.
   */
  public NullableProperty(@NotNull AbstractToStringProperty<T> internalProperty, @NotNull Predicate<T> acceptableValues) {
    this.internalProperty = internalProperty;
    this.acceptableValues = it -> it != null && acceptableValues.test(it);
  }

  /**
   * @return the value of {@link #internalProperty}, if {@link #acceptableValues}
   *   returns {@code true} for that value. Otherwise {@code null} will be returned.
   * @see AbstractToStringProperty#get()
   */
  @Nullable
  public T get() {
    return Optional.ofNullable(internalProperty.get()).filter(acceptableValues).orElse(null);
  }

  /**
   * <p>If the given value is {@code null}, the {@link #internalProperty} is always set to {@code null}.</p>
   *
   * <p>For other values, the value is passed to {@link #acceptableValues}. If that predicate evaluates to {@code true},
   * the {@link #internalProperty} is set to that value. If the predicate evaluates to {@code false},
   * the property is set to {@code null}.</p>
   *
   * @param value the new value of {@link #internalProperty}, if it matches {@link #acceptableValues}
   * @return {@code true}, if {@link #internalProperty} has changed, same as {@link AbstractToStringProperty#put(Object)}
   * @see AbstractToStringProperty#put(Object)
   */
  public boolean put(@Nullable T value) {
    return internalProperty.put(Optional.ofNullable(value).filter(acceptableValues).orElse(null));
  }

  /**
   * Simple delegate to {@link AbstractToStringProperty#addListener(AbstractProperty.ValueChangeListener)}
   * @param listener the listener to add
   */
  public void addListener(final AbstractProperty.ValueChangeListener<? super T> listener) {
    internalProperty.addListener(listener);
  }
}
