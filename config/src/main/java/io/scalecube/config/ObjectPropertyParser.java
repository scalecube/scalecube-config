package io.scalecube.config;

import io.scalecube.config.utils.ThrowableUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class ObjectPropertyParser {

  private ObjectPropertyParser() {
    // Do not instantiate
  }

  static <T> T parse(List<PropertyNameAndValue> nameValueList, List<ObjectPropertyField> fields, Class<T> objClass) {
    T instance;
    try {
      instance = objClass.newInstance();
    } catch (Exception e) {
      throw ThrowableUtil.propagate(e);
    }

    Map<String, Optional<String>> nameValueMap = nameValueList.stream()
        .collect(Collectors.toMap(PropertyNameAndValue::getName, PropertyNameAndValue::getValue));

    for (ObjectPropertyField propertyField : fields) {
      Optional<String> valueOptional = nameValueMap.get(propertyField.getPropertyName());
      if (valueOptional != null && valueOptional.isPresent()) {
        propertyField.applyValue(instance, valueOptional.get());
      }
    }

    return instance;
  }
}
