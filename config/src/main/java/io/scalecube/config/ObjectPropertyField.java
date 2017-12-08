package io.scalecube.config;

import io.scalecube.config.utils.ThrowableUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Helper holder class. Contains parsed field of the corresponding object class, associated property name and computed
 * {@link #valueParser} function.
 */
class ObjectPropertyField {
  private final Field field;
  private final String propertyName;
  private final Function<String, ?> valueParser;

  ObjectPropertyField(Field field, String propertyName) {
    int modifiers = field.getModifiers();
    if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
      throw new IllegalArgumentException(
          "ObjectPropertyField: 'static' or 'final' declaration is not supported (field: " + field + ")");
    }

    this.field = field;
    field.setAccessible(true); // set accessible to overcome 'private' declaration
    this.propertyName = propertyName;

    if (field.getGenericType() instanceof ParameterizedType) {
      ParameterizedType paramType = (ParameterizedType) field.getGenericType();
      if (isList(paramType)) {
        Type type = paramType.getActualTypeArguments()[0];
        this.valueParser = ListConfigPropertyImpl.toListPropertyParser(getValueParser(type));
      } else if (isMultimap(paramType)) {
        Type[] typeArguments = paramType.getActualTypeArguments();
        ParameterizedType valueType = ((ParameterizedType) typeArguments[1]);
        Type type = valueType.getActualTypeArguments()[0];
        this.valueParser = MultimapConfigPropertyImpl.toMultimapPropertyParser(getValueParser(type));
      } else {
        throw new IllegalArgumentException("ObjectPropertyField: unsupported type on field: " + field);
      }
    } else {
      this.valueParser = getValueParser(field.getType());
    }
  }

  private boolean isList(ParameterizedType paramType) {
    return paramType.getRawType() == List.class;
  }

  private boolean isMultimap(ParameterizedType paramType) {
    boolean result = false;
    if (paramType.getRawType() == Map.class) {
      Type[] typeArguments = paramType.getActualTypeArguments();
      Type keyType = typeArguments[0];
      Type valueType = typeArguments[1];
      if (keyType == String.class && valueType instanceof ParameterizedType) {
        ParameterizedType parameterizedValueType = ((ParameterizedType) valueType);
        if (parameterizedValueType.getRawType() == List.class) {
          result = true;
        }
      }
    }
    return result;
  }

  private Function<String, ?> getValueParser(Type type) {
    if (type == String.class) {
      return str -> str;
    } else if (type == Duration.class) {
      return DurationParser::parseDuration;
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

  String getPropertyName() {
    return propertyName;
  }

  void applyValueParser(Object instance, String value) {
    try {
      field.set(instance, valueParser.apply(value));
    } catch (IllegalAccessException e) {
      throw ThrowableUtil.propagate(e);
    }
  }
}
