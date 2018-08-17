package io.scalecube.config;

import io.scalecube.config.source.LoadedConfigProperty;
import java.util.Map;

class LongConfigPropertyImpl extends AbstractSimpleConfigProperty<Long>
    implements LongConfigProperty {

  LongConfigPropertyImpl(
      String name,
      Map<String, LoadedConfigProperty> propertyMap,
      Map<String, Map<Class, PropertyCallback>> propertyCallbackMap) {
    super(name, Long.class, propertyMap, propertyCallbackMap, ConfigRegistryImpl.LONG_PARSER);
  }

  @Override
  public long value(long defaultValue) {
    return value().orElse(defaultValue);
  }

  @Override
  public long valueOrThrow() {
    return value().orElseThrow(this::newNoSuchElementException);
  }
}
