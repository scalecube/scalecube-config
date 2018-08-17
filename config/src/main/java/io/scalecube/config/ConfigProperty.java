package io.scalecube.config;

import java.util.Optional;

/**
 * Single valued config property. Subclasses support value types: string, int, double, long,
 * duration, boolean and comma separated list. This type of property is suitable for configs having
 * single independent value with independent validation and independent callback function:
 *
 * <pre>
 * IntConfigProperty prop = ...
 * prop.addValidator(i -> i >= 42);
 * prop.addCallback((i0, i) -> doSometing(i));
 * </pre>
 *
 * <p>In example above validator and callback dont need other config values, they are fine with
 * single value {@code i} (though, for callback, there's also and old {@code i}).
 */
public interface ConfigProperty {

  /**
   * Returns value.
   *
   * @return property name, never null.
   */
  String name();

  /**
   * Info from what config source this property had been loaded: classpath, file directory, mongodb
   * and etc.
   *
   * @return optional config source name.
   */
  Optional<String> source();

  /**
   * Info from what concrete place this property had been loaded: path to file, mongo db table and
   * etc.
   *
   * @return optional origin string.
   */
  Optional<String> origin();

  /**
   * Returns optional string value.
   *
   * @return optional raw string value.
   */
  Optional<String> valueAsString();

  /**
   * Shortcut on {@code valueAsString().orElse(defaultValue)}.
   *
   * @return existing value or default
   */
  String valueAsString(String defaultValue);
}
