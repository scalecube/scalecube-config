package io.scalecube.config.source;

import io.scalecube.config.ConfigProperty;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public final class SystemPropertiesConfigSource implements ConfigSource {
  private Map<String, ConfigProperty> loadedConfig;

  private final ConfigSource configSource;

  public SystemPropertiesConfigSource() {
    this(null);
  }

  public SystemPropertiesConfigSource(ConfigSource configSource) {
    this.configSource = configSource;
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    if (loadedConfig != null) {
      return loadedConfig;
    }

    Properties properties = System.getProperties();
    if (configSource != null) {
      properties = mergeSystemProperties(configSource.loadConfig(), properties);
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

  private static Properties mergeSystemProperties(
      Map<String, ConfigProperty> overrideConfig, Properties properties) {

    final Properties finalProperties = new Properties();

    overrideConfig.values().stream()
        .filter(p -> p.valueAsString().isPresent())
        .forEach(p -> finalProperties.put(p.name(), p.valueAsString(null)));

    finalProperties.putAll(properties);

    finalProperties.forEach((key, value) -> System.setProperty((String) key, (String) value));

    return finalProperties;
  }
}
