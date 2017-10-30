package io.scalecube.config;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Multi valued typed config property. Purpose is to support logical groups of config values related to certain
 * configurable component, plus provide their atomic value retrieval, atomic validation, and atomic reload. For example:
 * 
 * <pre>
 * class ConnectorSettings {
 *   String user; // -> 'com.acme.connector.user'
 *   String password; // -> 'com.acme.connector.password'
 * }
 *
 * ObjectConfigProperty&lt;ConnectorSettings&gt; prop = ...
 * prop.addValidator(settings -> checkConnectorCredentialsValid(settings.user, settings.password);
 * prop.addCallback((settings0, settings) -> newConnector(settings.user, settings.password));
 * </pre>
 * 
 * @param <T> object config type (must have default constructor)
 */
public interface ObjectConfigProperty<T> {

  /**
   * @return config class name, never null.
   */
  String name();

  /**
   * @return optional object value.
   */
  Optional<T> value();

  /**
   * Shortcut on {@code value().orElse(defaultValue)}
   */
  T value(T defaultValue);

  /**
   * Adds reload callback to the list. Callbacks will be invoked in the order they were added, and only after validation
   * have been passed.
   *
   * @param callback reload callback, 1st argument is old value 2nd one is new value, both are nullable; though callback
   *        may throw exception, this wouldn't stop other callbacks from execution.
   */
  void addCallback(BiConsumer<T, T> callback);

  /**
   * Adds reload callback to the list. Callbacks will be invoked in the order they were added, and only after validation
   * have been passed.
   *
   * @param executor executor where reload callback will be executed.
   * @param callback reload callback, 1st argument is old value 2nd one is new value, both are nullable; though callback
   *        may throw exception, this wouldn't stop other callbacks from execution.
   */
  void addCallback(Executor executor, BiConsumer<T, T> callback);

  /**
   * Adds validator to the list of validators. Validators will be invoked in the order they were added. An argument to
   * predicate is nullable.
   *
   * @throws IllegalArgumentException in case existing value fails against passed {@code validator}.
   */
  void addValidator(Predicate<T> validator);
}
