package io.scalecube.config.utils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ObjectPropertyField {
  private final Field field;
  private final String propertyName;
  private final Function<String, Object> valueParser;

  public ObjectPropertyField(Field field, String propertyName) {
    this.field = field;
    field.setAccessible(true);
    this.propertyName = propertyName;

    if (field.getGenericType() instanceof ParameterizedType) {
      ParameterizedType paramType = (ParameterizedType) field.getGenericType();
      // check that only List type supported
      if (paramType.getRawType() != List.class) {
        throw new IllegalArgumentException("ObjectPropertyField: unsupported type on field: " + field);
      }
      // determine value parser for element type of the list
      Type type1 = paramType.getActualTypeArguments()[0];
      Function<String, Object> valueParser = getValueParser(type1);
      this.valueParser = str -> Arrays.stream(str.split(",")).map(valueParser).collect(Collectors.toList());
    } else {
      this.valueParser = getValueParser(field.getType());
    }
  }

  private Function<String, Object> getValueParser(Type type) {
    if (type == String.class) {
      return str -> str;
    } else if (type == Duration.class) {
      return DurationParser::parse;
    } else if (type == Integer.TYPE || type == Integer.class) {
      return Integer::parseInt;
    } else if (type == Double.TYPE || type == Double.class) {
      return Double::parseDouble;
    } else if (type == Boolean.TYPE || type == Boolean.class) {
      return Boolean::parseBoolean;
    } else if (type == Long.TYPE || type == Long.class) {
      return Long::parseLong;
    } else {
      throw new IllegalArgumentException("ObjectPropertyField: unsupported type on field: " + field);
    }
  }

  public String getPropertyName() {
    return propertyName;
  }

  void applyValue(Object instance, String value) {
    try {
      field.set(instance, valueParser.apply(value));
    } catch (IllegalAccessException e) {
      throw ThrowableUtil.propagate(e);
    }
  }
}
