package io.scalecube.config;

import io.scalecube.config.source.LoadedConfigProperty;
import io.scalecube.config.utils.ThrowableUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @param <T> type of the property value
 */
class ObjectConfigPropertyImpl<T> extends AbstractConfigProperty<T> implements ObjectConfigProperty<T> {

  ObjectConfigPropertyImpl(Map<String, String> bindingMap,
      Class<T> cfgClass,
      Map<String, LoadedConfigProperty> propertyMap,
      Map<String, Map<Class, PropertyCallback>> propertyCallbackMap) {

    super(cfgClass.getName(), cfgClass);

    List<ObjectPropertyField> propertyFields = toPropertyFields(bindingMap, cfgClass);
    setPropertyCallback(computePropertyCallback(cfgClass, propertyFields, propertyCallbackMap));

    computeValue(propertyFields.stream()
        .map(ObjectPropertyField::getPropertyName)
        .filter(propertyMap::containsKey)
        .map(propertyMap::get)
        .collect(Collectors.toList()));
  }

  @Override
  public T value(T defaultValue) {
    return value().orElse(defaultValue);
  }

  private List<ObjectPropertyField> toPropertyFields(Map<String, String> bindingMap, Class<T> cfgClass) {
    List<ObjectPropertyField> propertyFields = new ArrayList<>(bindingMap.size());
    for (String fieldName : bindingMap.keySet()) {
      Field field;
      try {
        field = cfgClass.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        throw ThrowableUtil.propagate(e);
      }
      int modifiers = field.getModifiers();
      if (!Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
        propertyFields.add(new ObjectPropertyField(field, bindingMap.get(fieldName)));
      }
    }
    return propertyFields;
  }

  private PropertyCallback<T> computePropertyCallback(Class<T> cfgClass,
      List<ObjectPropertyField> propertyFields,
      Map<String, Map<Class, PropertyCallback>> propertyCallbackMap) {

    PropertyCallback<T> propertyCallback =
        new PropertyCallback<>(list -> ObjectPropertyParser.parseObject(list, propertyFields, cfgClass));

    List<String> propertyNames = propertyFields.stream()
        .map(ObjectPropertyField::getPropertyName)
        .collect(Collectors.toList());

    // ensure that only one propertyCallback instance will be shared among instances of the same type
    synchronized (propertyCallbackMap) {
      propertyNames.forEach(propName -> {
        propertyCallbackMap.putIfAbsent(propName, new ConcurrentHashMap<>());
        Map<Class, PropertyCallback> callbackMap = propertyCallbackMap.get(propName);
        callbackMap.putIfAbsent(propertyClass, propertyCallback);
      });
    }

    // noinspection unchecked
    return propertyCallbackMap.values().stream()
        .filter(callbackMap -> callbackMap.containsKey(propertyClass))
        .map(callbackMap -> callbackMap.get(propertyClass))
        .collect(Collectors.toSet())
        .iterator()
        .next();
  }
}
