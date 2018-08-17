package io.scalecube.config;

import io.scalecube.config.source.LoadedConfigProperty;
import java.util.Map;

class StringConfigPropertyImpl extends AbstractSimpleConfigProperty<String>
    implements StringConfigProperty {

  StringConfigPropertyImpl(
      String name,
      Map<String, LoadedConfigProperty> propertyMap,
      Map<String, Map<Class, PropertyCallback>> propertyCallbackMap) {
    super(name, String.class, propertyMap, propertyCallbackMap, ConfigRegistryImpl.STRING_PARSER);
  }

  @Override
  public String value(String defaultValue) {
    return super.valueAsString(defaultValue);
  }

  @Override
  public String valueOrThrow() {
    return value().orElseThrow(this::newNoSuchElementException);
  }
}
