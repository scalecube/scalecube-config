package io.scalecube.config;

import io.scalecube.config.source.LoadedConfigProperty;
import java.util.Map;

class DoubleConfigPropertyImpl extends AbstractSimpleConfigProperty<Double>
    implements DoubleConfigProperty {

  DoubleConfigPropertyImpl(
      String name,
      Map<String, LoadedConfigProperty> propertyMap,
      Map<String, Map<Class, PropertyCallback>> propertyCallbackMap) {
    super(name, Double.class, propertyMap, propertyCallbackMap, ConfigRegistryImpl.DOUBLE_PARSER);
  }

  @Override
  public double value(double defaultValue) {
    return value().orElse(defaultValue);
  }

  @Override
  public double valueOrThrow() {
    return value().orElseThrow(this::newNoSuchElementException);
  }
}
