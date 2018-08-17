package io.scalecube.config.jmx;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.utils.ObjectMapperHolder;
import io.scalecube.config.utils.ThrowableUtil;
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
        .map(this::writeValueAsString)
        .collect(Collectors.toList());
  }

  @Override
  public Collection<String> getSources() {
    return configRegistry
        .getConfigSources()
        .stream()
        .map(this::writeValueAsString)
        .collect(Collectors.toList());
  }

  @Override
  public Collection<String> getEvents() {
    return configRegistry
        .getRecentConfigEvents()
        .stream()
        .map(this::writeValueAsString)
        .collect(Collectors.toList());
  }

  @Override
  public Collection<String> getSettings() {
    return Collections.singletonList(writeValueAsString(configRegistry.getSettings()));
  }

  private String writeValueAsString(Object input) {
    try {
      return ObjectMapperHolder.getInstance().writeValueAsString(input);
    } catch (JsonProcessingException e) {
      throw ThrowableUtil.propagate(e);
    }
  }
}
