package io.scalecube.config;

import io.scalecube.config.source.LoadedConfigProperty;
import java.util.Map;

class IntConfigPropertyImpl extends AbstractSimpleConfigProperty<Integer>
    implements IntConfigProperty {

  IntConfigPropertyImpl(
      String name,
      Map<String, LoadedConfigProperty> propertyMap,
      Map<String, Map<Class, PropertyCallback>> propertyCallbackMap) {
    super(name, Integer.class, propertyMap, propertyCallbackMap, ConfigRegistryImpl.INT_PARSER);
  }

  @Override
  public int value(int defaultValue) {
    return value().orElse(defaultValue);
  }

  @Override
  public int valueOrThrow() {
    return value().orElseThrow(this::newNoSuchElementException);
  }
}
