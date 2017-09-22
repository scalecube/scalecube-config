package io.scalecube.config;

import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.LoadedConfigProperty;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

public class TestUtil {

  public static final int RELOAD_PERIOD_SEC = 1;
  public static final long WAIT_FOR_RELOAD_PERIOD_MILLIS = RELOAD_PERIOD_SEC * 1500;

  public static Map<String, ConfigProperty> toConfigProps(Map<String, String> props) {
    Map<String, ConfigProperty> propertyMap = new HashMap<>();
    for (Map.Entry<String, String> entry : props.entrySet()) {
      propertyMap.put(entry.getKey(), LoadedConfigProperty.forNameAndValue(entry.getKey(), entry.getValue()));
    }
    return propertyMap;
  }

  public static ConfigRegistryImpl newConfigRegistry(ConfigSource configSource) {
    ConfigRegistryImpl configRegistry;
    configRegistry = new ConfigRegistryImpl(ConfigRegistrySettings.builder()
        .jmxEnabled(false)
        .keepRecentConfigEvents(0)
        .addLastSource("source", configSource)
        .reloadIntervalSec(RELOAD_PERIOD_SEC)
        .build());
    configRegistry.init();
    return configRegistry;
  }

  public static ImmutableMap.Builder<String, String> mapBuilder() {
    return ImmutableMap.builder();
  }
}
