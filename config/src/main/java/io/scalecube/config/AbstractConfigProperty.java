package io.scalecube.config;

import io.scalecube.config.source.LoadedConfigProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Abstract parent class for config property classes. Holds mutable state fields: {@link #value} the parsed config
 * property field, {@link #inputList} which is kind of a 'source' for parsed value. Reload process may change volatile
 * values of those fields, of course, if property value actually changed and validation passed. Collections of
 * validators and callbacks (of type {@link T}) are defined here and only operations around them are shared to
 * subclasses.
 *
 * @param <T> type of the property value
 */
abstract class AbstractConfigProperty<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfigProperty.class);

  private static final String ERROR_VALIDATION_FAILED =
      "Validation failed on config property: '%s', failed value: %s";

  final String name;
  final Class<?> propertyClass;
  final Collection<Predicate<T>> validators = new CopyOnWriteArraySet<>(); // of type Set for a reason
  final Collection<BiConsumer<T, T>> callbacks = new CopyOnWriteArraySet<>(); // of type Set for a reason

  private PropertyCallback<T> propertyCallback; // initialized from subclass
  private volatile T value; // initialized from subclass, reset in callback
  private volatile List<LoadedConfigProperty> inputList; // initialized from subclass, reset in callback

  AbstractConfigProperty(String name, Class<?> propertyClass) {
    this.name = name;
    this.propertyClass = propertyClass;
  }

  public final String name() {
    return name;
  }

  public final Optional<T> value() {
    return Optional.ofNullable(value);
  }

  public final void addValidator(Predicate<T> validator) {
    if (!validator.test(value)) {
      throw new IllegalArgumentException(String.format(ERROR_VALIDATION_FAILED, name, value));
    }
    validators.add(validator);
  }

  public final void addCallback(BiConsumer<T, T> callback) {
    callbacks.add((t1, t2) -> invokeCallback(callback, t1, t2));
  }

  public final void addCallback(Executor executor, BiConsumer<T, T> callback) {
    callbacks.add((t1, t2) -> executor.execute(() -> invokeCallback(callback, t1, t2)));
  }

  /**
   * Binds this config property instance to given {@link PropertyCallback}. The latter is shared among config property
   * instances of the same type.
   *
   * @param propertyCallback propertyCallback of certain type.
   */
  final void setPropertyCallback(PropertyCallback<T> propertyCallback) {
    (this.propertyCallback = propertyCallback).addConfigProperty(this);
  }

  /**
   * This method is being called from subclasses to assign a {@link #value} with initial value.
   * 
   * @see PropertyCallback#computeValue(List, AbstractConfigProperty)
   */
  final void computeValue(List<LoadedConfigProperty> inputList) {
    propertyCallback.computeValue(inputList, this);
  }

  /**
   * Takes new value, validates it, resets {@code this.value} field and optionally calling callbacks.
   * 
   * @param value1 new value to set; may be null.
   * @param inputList1 valueParser input list; contains additional info such as source, origin and string value
   *        representation which in fact had built up given {@code value1} param.
   * @param invokeCallbacks flag indicating whether it's needed to notify callbacks about changes.
   * @throws IllegalArgumentException in case new value fails against existing validators.
   */
  final void acceptValue(T value1, List<LoadedConfigProperty> inputList1, boolean invokeCallbacks) {
    if ((value == null && value1 == null) || isInputsEqual(inputList, inputList1)) {
      return;
    }

    if (!validators.stream().allMatch(input -> input.test(value1))) {
      throw new IllegalArgumentException(String.format(ERROR_VALIDATION_FAILED, name, value));
    }

    T t1 = value;
    T t2 = value = value1;

    inputList = inputList1;

    if (invokeCallbacks) {
      for (BiConsumer<T, T> callback : callbacks) {
        callback.accept(t1, t2);
      }
    }
  }

  /**
   * Helper method which applies given {@code mapper} lambda to the {@link #inputList} (if any). For example if one
   * needs to retrieve more than just a {@link #value} info from this config property, like source, origin and etc.
   */
  final Optional<String> mapToString(Function<List<LoadedConfigProperty>, String> mapper) {
    return Optional.ofNullable(inputList).map(mapper);
  }

  private void invokeCallback(BiConsumer<T, T> callback, T t1, T t2) {
    try {
      callback.accept(t1, t2);
    } catch (Exception e) {
      LOGGER.error(
          "Exception occurred on property-change callback: {}, property name: '{}', oldValue: {}, newValue: {}, cause: {}",
          callback, name, t1, t2, e, e);
    }
  }

  private boolean isInputsEqual(List<LoadedConfigProperty> inputList, List<LoadedConfigProperty> inputList1) {
    if ((inputList == null && inputList1 != null) || (inputList != null && inputList == null)) {
      return false;
    }

    Map<String, Optional<String>> inputMap = inputList.stream().collect(Collectors.toMap(
        LoadedConfigProperty::name, LoadedConfigProperty::valueAsString));

    Map<String, Optional<String>> inputMap1 = inputList1.stream().collect(Collectors.toMap(
        LoadedConfigProperty::name, LoadedConfigProperty::valueAsString));

    return inputMap.equals(inputMap1);
  }
}
