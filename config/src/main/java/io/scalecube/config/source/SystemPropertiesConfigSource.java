package io.scalecube.config.source;

import io.scalecube.config.ConfigProperty;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public final class SystemPropertiesConfigSource implements ConfigSource {
  private Map<String, ConfigProperty> loadedConfig;

  private final ConfigSource overrideConfigSource;

  public SystemPropertiesConfigSource() {
    this(null);
  }

  public SystemPropertiesConfigSource(ConfigSource overrideConfigSource) {
    this.overrideConfigSource = overrideConfigSource;
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    if (loadedConfig != null) {
      return loadedConfig;
    }

    Properties properties = System.getProperties();
    if (overrideConfigSource != null) {
      properties = overrideSystemProperties(overrideConfigSource.loadConfig(), properties);
    }

    Map<String, ConfigProperty> result = new TreeMap<>();

    for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements(); ) {
      String propName = (String) e.nextElement();
      result.put(
          propName,
          LoadedConfigProperty.forNameAndValue(propName, properties.getProperty(propName)));
    }

    return loadedConfig = result;
  }

  private static Properties overrideSystemProperties(
      Map<String, ConfigProperty> overrideConfig, Properties properties) {

    final Properties finalProperties = new Properties(properties);

    overrideConfig.values().stream()
        .filter(p -> p.valueAsString().isPresent())
        .forEach(
            p -> {
              String name = p.name();
              String value = p.valueAsString(null);
              finalProperties.put(name, value);
              System.setProperty(name, value);
            });

    return finalProperties;
  }
}
