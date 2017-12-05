package io.scalecube.config;

import io.scalecube.config.source.LoadedConfigProperty;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

class RangeConfigPropertyImpl<T extends Comparable<T>> extends AbstractSimpleConfigProperty<RangeConfigProperty.Range<T>> implements RangeConfigProperty<T> {

  RangeConfigPropertyImpl(String name,
                          Map<String, LoadedConfigProperty> propertyMap,
                          Map<String, Map<Class, PropertyCallback>> propertyCallbackMap,
                          Function<String, T> valueParser) {
    super(name, getListPropertyClass(valueParser), propertyMap, propertyCallbackMap, toRangePropertyParser(valueParser));
  }

  @Override
  public Range<T> value(Range<T> defaultValue) {
    return value().orElse(defaultValue);
  }

  @Override
  public Range<T> valueOrThrow() {
    return value().orElseThrow(this::newNoSuchElementException);
  }

  private static <T extends Comparable<T>> Function<String, Range<T>> toRangePropertyParser(Function<String, T> valueParser) {
    return str -> {
      String[] ranges = str.split("\\.\\.");
      return new Range<>(valueParser.apply(ranges[0].trim()), valueParser.apply(ranges[1].trim()));
    };
  }

  private static <T> Class<?> getListPropertyClass(Function<String, T> valueParser) {
    Class<?> result = null;
    if (ConfigRegistryImpl.DOUBLE_PARSER == valueParser) {
      result = Double.class;
    } else if (ConfigRegistryImpl.LONG_PARSER == valueParser) {
      result = Long.class;
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
