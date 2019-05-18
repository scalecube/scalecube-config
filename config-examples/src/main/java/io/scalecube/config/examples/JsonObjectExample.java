package io.scalecube.config.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.audit.Slf4JConfigEventListener;
import io.scalecube.config.source.SystemPropertiesConfigSource;
import io.scalecube.config.utils.ThrowableUtil;
import java.util.function.Function;

public class JsonObjectExample {

  private static final int RELOAD_INTERVAL_SEC = 3;

  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Main method of example of how to read json value from config registry.
   *
   * @param args program arguments
   */
  public static void main(String[] args) {
    System.setProperty("jsonKey", "{\"name\":\"property\",\"value\":1322134}");

    ConfigRegistrySettings configRegistrySettings =
        ConfigRegistrySettings.builder()
            .reloadIntervalSec(RELOAD_INTERVAL_SEC)
            .jmxEnabled(false)
            .addListener(new Slf4JConfigEventListener())
            .addLastSource("systemProperties", new SystemPropertiesConfigSource())
            .build();

    ConfigRegistry configRegistry = ConfigRegistry.create(configRegistrySettings);

    JsonEntity entity =
        configRegistry.objectProperty("jsonKey", mapper(JsonEntity.class)).value(null);
    System.out.println("entity = " + entity);
  }

  static class JsonEntity {
    private String name;
    private Integer value;

    public void setName(String name) {
      this.name = name;
    }

    public void setValue(Integer value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return name + ":" + value;
    }
  }

  private static <T> Function<String, T> mapper(Class<T> clazz) {
    return value -> {
      try {
        return objectMapper.readValue(value, clazz);
      } catch (Exception e) {
        throw ThrowableUtil.propagate(e);
      }
    };
  }
}
