package io.scalecube.config.source;

import io.scalecube.config.ConfigProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Stream;

public class SystemPropertiesConfigSource implements ConfigSource {
  private final List<String> namespaces;

  public SystemPropertiesConfigSource() {
    this.namespaces = Collections.emptyList();
  }

  public SystemPropertiesConfigSource(List<String> namespaces) {
    this.namespaces = new ArrayList<>(namespaces);
  }

  public SystemPropertiesConfigSource(String... namespaces) {
    this.namespaces = Arrays.asList(namespaces);
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    Properties properties = System.getProperties();
    Map<String, ConfigProperty> result = new TreeMap<>();
    Stream<String> namespaces1 = namespaces.stream();

    for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements(); ) {
      String propName = (String) e.nextElement();
      if (namespaces.isEmpty() || namespaces1.anyMatch(propName::startsWith)) {
        result.put(
            propName,
            LoadedConfigProperty.forNameAndValue(propName, properties.getProperty(propName)));
      }
    }
    return result;
  }
}
