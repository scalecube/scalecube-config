package io.scalecube.config.jmx;

import io.scalecube.config.ConfigPropertyInfo;
import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.audit.ConfigEvent;
import io.scalecube.config.source.ConfigSourceInfo;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class JmxConfigRegistry implements JmxConfigRegistryMBean {

  private final ConfigRegistry configRegistry;

  public JmxConfigRegistry(ConfigRegistry configRegistry) {
    this.configRegistry = configRegistry;
  }

  @Override
  public Collection<String> getProperties() {
    return configRegistry
        .getConfigProperties()
        .stream()
        .map(ConfigPropertyInfo::toString)
        .collect(Collectors.toList());
  }

  @Override
  public Collection<String> getSources() {
    return configRegistry
        .getConfigSources()
        .stream()
        .map(ConfigSourceInfo::toString)
        .collect(Collectors.toList());
  }

  @Override
  public Collection<String> getEvents() {
    return configRegistry
        .getRecentConfigEvents()
        .stream()
        .map(ConfigEvent::toString)
        .collect(Collectors.toList());
  }

  @Override
  public Collection<String> getSettings() {
    return Collections.singletonList(configRegistry.getSettings().toString());
  }
}
