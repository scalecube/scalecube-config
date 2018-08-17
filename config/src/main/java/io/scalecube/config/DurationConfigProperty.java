package io.scalecube.config;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public interface DurationConfigProperty extends ConfigProperty {

  /**
   * Returns value.
   *
   * @return optional duration value
   */
  Optional<Duration> value();

  /**
   * Shortcut on {@code value().orElse(defaultValue)}.
   *
   * @return existing value or default
   */
  Duration value(Duration defaultValue);

  /**
   * Returns existing value or throws {@link NoSuchElementException} if value is null.
   *
   * @return existing value or exception
   * @throws NoSuchElementException if value is null
   */
  Duration valueOrThrow();

  /**
   * Adds reload callback to the list. Callbacks will be invoked in the order they were added, and
   * only after validation have been passed.
   *
   * @param callback reload callback, 1st argument is old value 2nd one is new value, both are
   *     nullable; though callback may throw exception, this wouldn't stop other callbacks from
   *     execution
   */
  void addCallback(BiConsumer<Duration, Duration> callback);

  /**
   * Adds reload callback to the list. Callbacks will be invoked in the order they were added, and
   * only after validation have been passed.
   *
   * @param executor executor where reload callback will be executed.
   * @param callback reload callback, 1st argument is old value 2nd one is new value, both are
   *     nullable; though callback may throw exception, this wouldn't stop other callbacks from
   *     execution
   */
  void addCallback(Executor executor, BiConsumer<Duration, Duration> callback);

  /**
   * Adds validator to the list of validators. Validators will be invoked in the order they were
   * added. An argument to predicate is nullable.
   *
   * @throws IllegalArgumentException in case existing value fails against passed {@code validator}
   */
  void addValidator(Predicate<Duration> validator);
}
