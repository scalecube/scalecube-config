package io.scalecube.config;

import io.scalecube.config.source.LoadedConfigProperty;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class ListConfigPropertyImpl<T> extends AbstractSimpleConfigProperty<List<T>> implements ListConfigProperty<T> {

  ListConfigPropertyImpl(String name,
      Map<String, LoadedConfigProperty> propertyMap,
      Map<String, Map<Class, PropertyCallback>> propertyCallbackMap,
      Function<String, T> valueParser) {
    super(name, getListPropertyClass(valueParser), propertyMap, propertyCallbackMap, toListPropertyParser(valueParser));
  }

  @Override
  public List<T> value(List<T> defaultValue) {
    return value().orElse(defaultValue);
  }

  @Override
  public List<T> valueOrThrow() {
    return value().orElseThrow(this::newNoSuchElementException);
  }

  private static <T> Function<String, List<T>> toListPropertyParser(Function<String, T> valueParser) {
    return str -> Arrays.stream(str.split(",")).map(valueParser).collect(Collectors.toList());
  }

  private static <T> Class<?> getListPropertyClass(Function<String, T> valueParser) {
    Class<?> result = null;
    if (ConfigRegistryImpl.STRING_PARSER == valueParser) {
      result = String.class;
    } else if (ConfigRegistryImpl.DOUBLE_PARSER == valueParser) {
      result = Double.class;
    } else if (ConfigRegistryImpl.LONG_PARSER == valueParser) {
      result = Long.class;
    } else if (ConfigRegistryImpl.BOOLEAN_PARSER == valueParser) {
      result = Boolean.class;
    } else if (ConfigRegistryImpl.INT_PARSER == valueParser) {
      result = Integer.class;
    } else if (ConfigRegistryImpl.DURATION_PARSER == valueParser) {
      result = Duration.class;
    }
    if (result == null) {
      throw new IllegalArgumentException("ListConfigPropertyImpl: unsupported list valueParser " + valueParser);
    }
    return result;
  }
}
