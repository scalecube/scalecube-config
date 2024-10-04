package io.scalecube.config;

import io.scalecube.config.audit.ConfigEvent;
import io.scalecube.config.source.LoadedConfigProperty;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Property controller class for config property instances of type {@link T}. Config property
 * instances of the same type are managed by exactly one property controller.
 *
 * @param <T> type of the property value
 */
class PropertyCallback<T> {

  private static final Logger LOGGER = Logger.getLogger(PropertyCallback.class.getName());

  private static final String ERROR_EXCEPTION_ON_VALUE_PARSER =
      "Exception occurred at valueParser on input: %s, cause: %s";
  private static final String ERROR_EXCEPTION_AT_ACCEPT_VALUE =
      "Exception occurred at acceptValue on input: %s, on value: %s, cause: %s";

  /**
   * Value parser function. Converts list of string name-value pairs (input list can be null or
   * empty, in this case function would simply return null) to concrete object, i.e. a config
   * property value.
   */
  private final Function<List<LoadedConfigProperty>, T> valueParser;

  /**
   * Collection of ConfigProperty objects of the same type assigned to this {@link
   * PropertyCallback}.
   */
  private final Collection<AbstractConfigProperty<T>> configProperties =
      new CopyOnWriteArraySet<>();

  /**
   * Creates property callback.
   *
   * @param valueParser value parser for config property object of certain type.
   */
  PropertyCallback(Function<List<LoadedConfigProperty>, T> valueParser) {
    this.valueParser = list -> list == null || list.isEmpty() ? null : valueParser.apply(list);
  }

  /** Just adds config property instance to internal collection. */
  void addConfigProperty(AbstractConfigProperty<T> configProperty) {
    configProperties.add(configProperty);
  }

  /**
   * Computes new value for config property instances (of type {@link T}) from the list of {@link
   * ConfigEvent}-s. This method is being called from config registry reload process.
   *
   * @param events config events computed during config registry reload.
   */
  void computeValue(List<ConfigEvent> events) {
    List<LoadedConfigProperty> inputList =
        events.stream()
            .filter(
                event ->
                    event.getType()
                        != ConfigEvent.Type.REMOVED) // we only interested in ADDED or UPDATED
            .map(
                event ->
                    LoadedConfigProperty.withNameAndValue(event.getName(), event.getNewValue())
                        .source(event.getNewSource())
                        .origin(event.getNewOrigin())
                        .build())
            .collect(Collectors.toList());

    T value;
    try {
      value = applyValueParser(inputList);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e.getCause());
      return; // return right away if parser failed
    }

    T newValue = value; // new value
    configProperties.forEach(
        configProperty -> {
          try {
            configProperty.acceptValue(newValue, inputList, true /* invokeCallbacks */);
          } catch (Exception e) {
            LOGGER.log(
                Level.SEVERE,
                String.format(ERROR_EXCEPTION_AT_ACCEPT_VALUE, inputList, newValue, e));
          }
        });
  }

  /**
   * Computes new value for config property instance (passed as second parameter) from the list of
   * {@link LoadedConfigProperty}-s.
   *
   * @param inputList an input for {@link #valueParser}.
   * @param configProperty an instance where attempt to set a new value has to be made.
   * @throws IllegalArgumentException in case value can't be parsed from the inputList or validation
   *     doesn't pass.
   */
  void computeValue(
      List<LoadedConfigProperty> inputList, AbstractConfigProperty<T> configProperty) {
    T value = applyValueParser(inputList);
    try {
      configProperty.acceptValue(value, inputList, false /* invokeCallbacks */);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          String.format(ERROR_EXCEPTION_AT_ACCEPT_VALUE, inputList, value, e));
    }
  }

  private T applyValueParser(List<LoadedConfigProperty> inputList) {
    try {
      return valueParser.apply(inputList);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          String.format(ERROR_EXCEPTION_ON_VALUE_PARSER, inputList, e));
    }
  }
}
