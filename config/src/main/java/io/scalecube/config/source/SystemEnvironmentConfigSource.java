package io.scalecube.config.source;

import io.scalecube.config.ConfigProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public class SystemEnvironmentConfigSource implements ConfigSource {
  private final List<String> namespaces;

  private Map<String, ConfigProperty> loadedConfig;

  public SystemEnvironmentConfigSource() {
    this.namespaces = Collections.emptyList();
  }

  public SystemEnvironmentConfigSource(List<String> namespaces) {
    this.namespaces = new ArrayList<>(namespaces);
  }

  public SystemEnvironmentConfigSource(String... namespaces) {
    this.namespaces = Arrays.asList(namespaces);
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    if (loadedConfig != null) {
      return loadedConfig;
    }

    Map<String, String> env = System.getenv();
    Map<String, ConfigProperty> result = new TreeMap<>();
    Stream<String> namespaces1 = namespaces.stream();

    for (Map.Entry<String, String> entry : env.entrySet()) {
      String propName = entry.getKey();
      if (namespaces.isEmpty() || namespaces1.anyMatch(propName::startsWith)) {
        result.put(propName, LoadedConfigProperty.forNameAndValue(propName, entry.getValue()));
      }
    }

    return loadedConfig = result;
  }
}
