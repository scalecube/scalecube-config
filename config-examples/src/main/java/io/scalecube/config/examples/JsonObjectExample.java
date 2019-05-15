package io.scalecube.config.examples;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.audit.Slf4JConfigEventListener;
import io.scalecube.config.source.SystemPropertiesConfigSource;

public class JsonObjectExample {

  private static final int RELOAD_INTERVAL_SEC = 3;

  public static void main(String[] args) {
    System.setProperty("jsonKey", "{\"name\":\"property\",\"value\":1322134}");
    System.setProperty("intKey", "12345");

    ConfigRegistrySettings configRegistrySettings =
        ConfigRegistrySettings.builder()
            .reloadIntervalSec(RELOAD_INTERVAL_SEC)
            .jmxEnabled(false)
            .addListener(new Slf4JConfigEventListener())
            .addLastSource("systemProperties", new SystemPropertiesConfigSource())
            .build();

    ConfigRegistry configRegistry = ConfigRegistry.create(configRegistrySettings);

    int intKey = configRegistry.intProperty("intKey").value(-1);
    System.out.println("intKey = " + intKey);

    JsonEntity entity = configRegistry.jsonObjectProperty("jsonKey", JsonEntity.class).value(null);
    System.out.println("entity = " + entity);
  }

  static class JsonEntity {
    private String name;
    private Integer value;

    @Override
    public String toString() {
      return name + ":" + value;
    }
  }
}
