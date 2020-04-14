package io.scalecube.config.source;

import io.scalecube.config.ConfigProperty;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;

public final class SystemEnvironmentVariablesConfigSource implements ConfigSource {

  private Map<String, ConfigProperty> loadedConfig;

  // from-to environment variables mappings
  private final Map<String, String> mappings = new TreeMap<>();

  public SystemEnvironmentVariablesConfigSource(Consumer<Mapper> consumer) {
    consumer.accept(new MapperImpl());
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    if (loadedConfig != null) {
      return loadedConfig;
    }

    Map<String, ConfigProperty> result = new TreeMap<>();

    System.getenv()
        .forEach(
            (propName, propValue) -> {
              String dst = mappings.get(propName);
              if (dst != null) {
                // store value as system property under mapped name
                System.setProperty(dst, propValue);
                result.put(dst, LoadedConfigProperty.forNameAndValue(dst, propValue));
              } else {
                result.put(propName, LoadedConfigProperty.forNameAndValue(propName, propValue));
              }
            });

    return loadedConfig = result;
  }

  // Stores mapping between environemnt variable name and corresponding system property name.
  public interface Mapper {

    Mapper map(String src, String dst);
  }

  // Inner implementation class
  private class MapperImpl implements Mapper {

    @Override
    public Mapper map(String src, String dst) {
      Objects.requireNonNull(src);
      Objects.requireNonNull(dst);
      mappings.put(src, dst);
      return this;
    }
  }
}
