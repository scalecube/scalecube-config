package io.scalecube.config.jmx;

import java.util.Collection;

public interface JmxConfigRegistryMBean {

  Collection<String> getProperties();

  Collection<String> getSources();

  Collection<String> getEvents();

  Collection<String> getSettings();
}
