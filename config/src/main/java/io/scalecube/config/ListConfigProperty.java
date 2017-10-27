package io.scalecube.config;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * List config property for comma separated values. Parsable types supported: string, int, double, long, duration,
 * boolean.
 * 
 * @param <T> type of list element
 */
public interface ListConfigProperty<T> extends ConfigProperty {

  /**
   * @return optional list value.
   */
  Optional<List<T>> value();

  /**
   * Shortcut on {@code value().orElse(defaultValue)}
   */
  List<T> value(List<T> defaultValue);

  /**
   * @throws NoSuchElementException if value is null
   */
  List<T> valueOrThrow();

  /**
   * Adds reload callback to the list. Callbacks will be invoked in the order they were added, and only after validation
   * have been passed.
   *
   * @param callback reload callback, 1st argument is old value 2nd one is new value, both are nullable; though callback
   *        may throw exception, this wouldn't stop other callbacks from execution.
   */
  void addCallback(BiConsumer<List<T>, List<T>> callback);

  /**
   * Adds reload callback to the list. Callbacks will be invoked in the order they were added, and only after validation
   * have been passed.
   *
   * @param executor executor where reload callback will be executed.
   * @param callback reload callback, 1st argument is old value 2nd one is new value, both are nullable; though callback
   *        may throw exception, this wouldn't stop other callbacks from execution.
   */
  void addCallback(Executor executor, BiConsumer<List<T>, List<T>> callback);

  /**
   * Adds validator to the list of validators. Validators will be invoked in the order they were added. An argument to
   * predicate is nullable.
   *
   * @throws IllegalArgumentException in case existing value fails against passed {@code validator}.
   */
  void addValidator(Predicate<List<T>> validator);
}
