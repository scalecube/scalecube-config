package io.scalecube.config.utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ObjectPropertyParser {

  private ObjectPropertyParser() {
    // Do not instantiate
  }

  public static <T> T parse(List<NameAndValue> nameAndValueList, List<ObjectPropertyField> fields, Class<T> objClass) {
    T instance;
    try {
      instance = objClass.newInstance();
    } catch (Exception e) {
      throw ThrowableUtil.propagate(e);
    }

    Map<String, Optional<String>> nameAndValueMap = nameAndValueList.stream()
        .collect(Collectors.toMap(NameAndValue::getName, NameAndValue::getValue));

    for (ObjectPropertyField propertyField : fields) {
      Optional<String> valueOptional = nameAndValueMap.get(propertyField.getPropertyName());
      if (valueOptional != null && valueOptional.isPresent()) {
        propertyField.applyValue(instance, valueOptional.get());
      }
    }

    return instance;
  }
}
