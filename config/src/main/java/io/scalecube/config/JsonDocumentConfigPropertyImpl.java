package io.scalecube.config;

import com.fasterxml.jackson.databind.ObjectReader;
import io.scalecube.config.source.LoadedConfigProperty;
import io.scalecube.config.utils.ObjectMapperHolder;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ObjectConfigProperty}.
 *
 * @param <T> type of the property value
 */
class JsonDocumentConfigPropertyImpl<T> extends AbstractConfigProperty<T>
    implements ObjectConfigProperty<T> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(JsonDocumentConfigPropertyImpl.class);
  private final ObjectReader reader;

  JsonDocumentConfigPropertyImpl(
      String documentKey,
      Class<T> cfgClass,
      Map<String, LoadedConfigProperty> propertyMap,
      Map<String, Map<Class, PropertyCallback>> propertyCallbackMap) {

    super(documentKey, cfgClass);
    reader = ObjectMapperHolder.getInstance().readerFor(cfgClass);
    PropertyCallback<T> propertyCallback = new PropertyCallback<>(this::valueParser);

    setPropertyCallback(propertyCallback);
    synchronized (propertyCallbackMap) {
      propertyCallbackMap
          .computeIfAbsent(documentKey, k -> new ConcurrentHashMap<>())
          .put(cfgClass, propertyCallback);
    }
    computeValue(
        Collections.singletonList(
            propertyMap.getOrDefault(
                documentKey, LoadedConfigProperty.forNameAndValue(documentKey, null))));
  }

  private T valueParser(List<LoadedConfigProperty> properties) {
    return properties.stream()
        .filter(this::isMyProperty)
        .findFirst()
        .flatMap(LoadedConfigProperty::valueAsString)
        .map(this::parse)
        .orElse(null);
  }

  private T parse(String source) {
    try {
      if (source != null) {
        return reader.readValue(source);
      }
      return null;
    } catch (IOException cause) {
      throw new IllegalArgumentException("JSON parse  failed", cause);
    }
  }

  @Override
  public T value(T defaultValue) {
    return value().orElse(defaultValue);
  }
}
