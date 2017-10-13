package io.scalecube.config;

import io.scalecube.config.source.LoadedConfigProperty;

import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Parent class for 'simple' property types, such as: double, int, string, list and etc.
 *
 * @param <T> type of the property value
 */
class AbstractSimpleConfigProperty<T> extends AbstractConfigProperty<T> implements ConfigProperty {

  /**
   * Constructor for non-object config property.
   */
  AbstractSimpleConfigProperty(String name,
      Class<?> propertyClass,
      Map<String, LoadedConfigProperty> propertyMap,
      Map<String, Map<Class, PropertyCallback>> propertyCallbackMap,
      Function<String, T> valueParser) {

    super(name, propertyClass);

    // noinspection unchecked
    setPropertyCallback(computePropertyCallback(valueParser, propertyCallbackMap));

    LoadedConfigProperty configProperty = propertyMap.get(name);
    computeValue(configProperty != null ? Collections.singletonList(configProperty) : null);
  }

  @Override
  public final Optional<String> source() {
    return mapToString(list -> list.get(0).source().orElse(null));
  }

  @Override
  public final Optional<String> origin() {
    return mapToString(list -> list.get(0).origin().orElse(null));
  }

  @Override
  public final Optional<String> valueAsString() {
    return mapToString(list -> list.get(0).valueAsString().orElse(null));
  }

  @Override
  public final String valueAsString(String defaultValue) {
    return valueAsString().orElse(defaultValue);
  }

  final NoSuchElementException newNoSuchElementException() {
    return new NoSuchElementException("Value is null for property '" + name + "'");
  }

  private PropertyCallback computePropertyCallback(Function<String, T> valueParser,
      Map<String, Map<Class, PropertyCallback>> propertyCallbackMap) {

    PropertyCallback<T> propertyCallback =
        new PropertyCallback<>(list -> list.get(0).valueAsString().map(valueParser).orElse(null));

    propertyCallbackMap.putIfAbsent(name, new ConcurrentHashMap<>());
    Map<Class, PropertyCallback> callbackMap = propertyCallbackMap.get(name);
    callbackMap.putIfAbsent(propertyClass, propertyCallback);
    return callbackMap.get(propertyClass);
  }
}
