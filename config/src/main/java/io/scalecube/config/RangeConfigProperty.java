package io.scalecube.config;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Range config property. Parsable types supported: int, double, long, duration.
 */
public interface RangeConfigProperty<T extends Comparable<T>> extends ConfigProperty {

  /**
   * @return optional range value.
   */
  Optional<Range<T>> value();

  /**
   * Shortcut on {@code value().orElse(defaultValue)}
   */
  Range<T> value(Range<T> defaultValue);

  /**
   * @throws NoSuchElementException if value is null
   */
  Range<T> valueOrThrow();

  /**
   * Adds reload callback to the list. Callbacks will be invoked in the order they were added, and only after validation
   * have been passed.
   *
   * @param callback reload callback, 1st argument is old value 2nd one is new value, both are nullable; though callback
   *                 may throw exception, this wouldn't stop other callbacks from execution.
   */
  void addCallback(BiConsumer<Range<T>, Range<T>> callback);

  /**
   * Adds reload callback to the list. Callbacks will be invoked in the order they were added, and only after validation
   * have been passed.
   *
   * @param executor executor where reload callback will be executed.
   * @param callback reload callback, 1st argument is old value 2nd one is new value, both are nullable; though callback
   *                 may throw exception, this wouldn't stop other callbacks from execution.
   */
  void addCallback(Executor executor, BiConsumer<Range<T>, Range<T>> callback);

  /**
   * Adds validator to the list of validators. Validators will be invoked in the order they were added. An argument to
   * predicate is nullable.
   *
   * @throws IllegalArgumentException in case existing value fails against passed {@code validator}.
   */
  void addValidator(Predicate<Range<T>> validator);

  class Range<T extends Comparable<T>> {
    private final T from;
    private final T to;

    public Range(T from, T to) {
      this.from = from;
      this.to = to;
    }

    public T getFrom() {
      return from;
    }

    public T getTo() {
      return to;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Range<?> range = (Range<?>) o;
      return Objects.equals(from, range.from) &&
        Objects.equals(to, range.to);
    }

    @Override
    public int hashCode() {
      return Objects.hash(from, to);
    }

    @Override
    public String toString() {
      return "Range{" +
        "from=" + from +
        ", to=" + to +
        '}';
    }
  }
}
