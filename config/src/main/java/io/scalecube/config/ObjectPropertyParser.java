package io.scalecube.config;

import io.scalecube.config.source.LoadedConfigProperty;
import io.scalecube.config.utils.ThrowableUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Parser for {@link ObjectConfigProperty}. Returns object instance of the given class by the list of
 * {@link LoadedConfigProperty}-s and list of {@link ObjectPropertyField}-s. The class must contain default constructor.
 */
class ObjectPropertyParser {

  private ObjectPropertyParser() {
    // Do not instantiate
  }

  static <T> T parseObject(List<LoadedConfigProperty> inputList,
      List<ObjectPropertyField> propertyFields,
      Class<T> cfgClass) {

    T instance;
    try {
      instance = cfgClass.newInstance();
    } catch (Exception e) {
      throw ThrowableUtil.propagate(e);
    }

    Map<String, Optional<String>> inputMap = inputList.stream()
        .collect(Collectors.toMap(LoadedConfigProperty::name, LoadedConfigProperty::valueAsString));

    for (ObjectPropertyField propertyField : propertyFields) {
      Optional<String> valueOptional = inputMap.get(propertyField.getPropertyName());
      if (valueOptional != null && valueOptional.isPresent()) {
        propertyField.applyValueParser(instance, valueOptional.get());
      }
    }
    return instance;
  }
}
