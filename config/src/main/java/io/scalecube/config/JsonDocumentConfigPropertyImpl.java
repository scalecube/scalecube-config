package io.scalecube.config;

import com.fasterxml.jackson.databind.ObjectReader;
import io.scalecube.config.source.LoadedConfigProperty;
import io.scalecube.config.utils.ObjectMapperHolder;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

/**
 * Implementation of {@link ObjectConfigProperty}.
 *
 * @param <T> type of the property value
 */
class JsonDocumentConfigPropertyImpl<T> extends AbstractConfigProperty<T>
    implements ObjectConfigProperty<T> {

  T value;

  JsonDocumentConfigPropertyImpl(String documentKey, Class<T> cfgClass) {
    super(documentKey, cfgClass);
    ObjectReader reader = ObjectMapperHolder.getInstance().readerFor(cfgClass);
    Function<String, T> safeRead =
        s -> {
          try {
            return value = reader.readValue(s);
          } catch (IOException ignoredException) {
            return null;
          }
        };
    setPropertyCallback(
        new PropertyCallback<>(
            l ->
                l.stream()
                    .filter(loadedConfig -> loadedConfig.name().equals(documentKey))
                    .findFirst()
                    .flatMap(LoadedConfigProperty::valueAsString)
                    .map(safeRead)
                    .orElse(null)));
    computeValue(
        Collections.singletonList(LoadedConfigProperty.forNameAndValue(documentKey, "{}")));
  }

  @Override
  public T value(T defaultValue) {
    return Optional.ofNullable(value).orElse(defaultValue);
  }
}
