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
  private final AbstractToStringProperty<T> delegate;
  private final Predicate<T> acceptableValues;
  private final T pseudoNull;

  /**
   * @see NullableProperty#NullableProperty(AbstractToStringProperty, Predicate, Object)
   */
  public NullableProperty(@NotNull AbstractToStringProperty<T> delegate, @NotNull Predicate<T> acceptableValues) {
    this(delegate, acceptableValues, null);
  }

  /**
   * @param delegate the property that should be treated as nullable
   * @param acceptableValues values for which this predicate returns true will be persisted as-is,
   *   all other values will be treated as {@code null}. The value {@code null} will always be treated
   *   as {@code null}, it is never passed to the predicate.
   * @param pseudoNull the value that is used internally to represent {@code null}. This is needed for properties,
   *   for which the default value is not {@code null}.
   */
  public NullableProperty(@NotNull AbstractToStringProperty<T> delegate, @NotNull Predicate<T> acceptableValues, final T pseudoNull) {
    this.delegate = delegate;
    this.acceptableValues = it -> it != null && acceptableValues.test(it);
    this.pseudoNull = pseudoNull;
  }

  /**
   * @return the value of {@link #delegate}, if {@link #acceptableValues}
   *   returns {@code true} for that value. Otherwise {@code null} will be returned.
   * @see AbstractToStringProperty#get()
   */
  @Nullable
  public T get() {
    return Optional.ofNullable(delegate.get()).filter(acceptableValues).orElse(null);
  }

  /**
   * <p>If the given value is {@code null}, the {@link #delegate} is always set to {@code null}.</p>
   *
   * <p>For other values, the value is passed to {@link #acceptableValues}. If that predicate evaluates to {@code true},
   * the {@link #delegate} is set to that value. If the predicate evaluates to {@code false},
   * the property is set to {@code null}.</p>
   *
   * @param value the new value of {@link #delegate}, if it matches {@link #acceptableValues}
   * @return {@code true}, if {@link #delegate} has changed, same as {@link AbstractToStringProperty#put(Object)}
   * @see AbstractToStringProperty#put(Object)
   */
  public boolean put(@Nullable T value) {
    return delegate.put(Optional.ofNullable(value).filter(acceptableValues).orElse(pseudoNull));
  }

  /**
   * Simple delegate to {@link AbstractToStringProperty#addListener(AbstractProperty.ValueChangeListener)}
   * @param listener the listener to add
   */
  public void addListener(final AbstractProperty.ValueChangeListener<? super T> listener) {
    delegate.addListener(listener);
  }
}
