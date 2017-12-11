package io.scalecube.config;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Multimap config property. Parsable value types supported: string, int, double, long, duration.
 */
public interface MultimapConfigProperty<V> extends ConfigProperty {
  /**
   * @return optional multimap value.
   */
  Optional<Map<String, List<V>>> value();

  /**
   * Shortcut on {@code value().orElse(defaultValue)}
   */
  Map<String, List<V>> value(Map<String, List<V>> defaultValue);

  /**
   * @throws NoSuchElementException if value is null
   */
  Map<String, List<V>> valueOrThrow();

  /**
   * Adds reload callback to the list. Callbacks will be invoked in the order they were added, and only after validation
   * have been passed.
   *
   * @param callback reload callback, 1st argument is old value 2nd one is new value, both are nullable; though callback
   *        may throw exception, this wouldn't stop other callbacks from execution.
   */
  void addCallback(BiConsumer<Map<String, List<V>>, Map<String, List<V>>> callback);

  /**
   * Adds reload callback to the list. Callbacks will be invoked in the order they were added, and only after validation
   * have been passed.
   *
   * @param executor executor where reload callback will be executed.
   * @param callback reload callback, 1st argument is old value 2nd one is new value, both are nullable; though callback
   *        may throw exception, this wouldn't stop other callbacks from execution.
   */
  void addCallback(Executor executor, BiConsumer<Map<String, List<V>>, Map<String, List<V>>> callback);

  /**
   * Adds validator to the list of validators. Validators will be invoked in the order they were added. An argument to
   * predicate is nullable.
   *
   * @throws IllegalArgumentException in case existing value fails against passed {@code validator}.
   */
  void addValidator(Predicate<Map<String, List<V>>> validator);
}
