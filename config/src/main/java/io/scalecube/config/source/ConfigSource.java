package io.scalecube.config.source;

import io.scalecube.config.ConfigProperty;

import java.util.Map;

/**
 * Config source interface which represents specific provider of configuration properties.
 */
public interface ConfigSource {

  /**
   * Loads all properties from the source.
   */
  Map<String, ConfigProperty> loadConfig();
}
