package io.scalecube.config;

import io.scalecube.config.source.LoadedConfigProperty;

import java.time.Duration;
import java.util.Map;

class DurationConfigPropertyImpl extends AbstractSimpleConfigProperty<Duration> implements DurationConfigProperty {

  DurationConfigPropertyImpl(String name,
      Map<String, LoadedConfigProperty> propertyMap,
      Map<String, Map<Class, PropertyCallback>> propertyCallbackMap) {
    super(name, Duration.class, propertyMap, propertyCallbackMap, ConfigRegistryImpl.DURATION_PARSER);
  }

  @Override
  public Duration value(Duration defaultValue) {
    return value().orElse(defaultValue);
  }

  @Override
  public Duration valueOrThrow() {
    return value().orElseThrow(this::newNoSuchElementException);
  }
}
