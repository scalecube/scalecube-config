package io.scalecube.config;

import io.scalecube.config.source.LoadedConfigProperty;

import java.util.Map;

class BooleanConfigPropertyImpl extends AbstractSimpleConfigProperty<Boolean> implements BooleanConfigProperty {

  BooleanConfigPropertyImpl(String name,
      Map<String, LoadedConfigProperty> propertyMap,
      Map<String, Map<Class, PropertyCallback>> propertyCallbackMap) {
    super(name, Boolean.class, propertyMap, propertyCallbackMap, ConfigRegistryImpl.BOOLEAN_PARSER);
  }

  @Override
  public boolean value(boolean defaultValue) {
    return value().orElse(defaultValue);
  }

  @Override
  public boolean valueOrThrow() {
    return value().orElseThrow(this::newNoSuchElementException);
  }
}
