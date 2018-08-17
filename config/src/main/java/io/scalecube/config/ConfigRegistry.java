package io.scalecube.config;

import io.scalecube.config.audit.ConfigEvent;
import io.scalecube.config.source.ConfigSourceInfo;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Config registry base facade interface.
 *
 * @author Anton Kharenko
 */
public interface ConfigRegistry {

  /**
   * Creates new instance of config registry with the given settings.
   *
   * @param settings config registry settings
   * @return config registry instance
   */
  static ConfigRegistry create(ConfigRegistrySettings settings) {
    ConfigRegistryImpl configRegistry = new ConfigRegistryImpl(settings);
    configRegistry.init();
    return configRegistry;
  }

  /**
   * Returns dynamic typed object property.
   *
   * @param prefix prefix of property keys extended with field names as prefix.fieldName to resolve
   *     full property keys
   * @param cfgClass a class of config object instance
   * @param <T> a type of config object
   * @return property instance
   */
  <T> ObjectConfigProperty<T> objectProperty(String prefix, Class<T> cfgClass);

  /**
   * Returns dynamic typed object property.
   *
   * @param bindingMap custom mapping between class field names and property names
   * @param cfgClass a class of config object instance
   * @param <T> a type of config object
   * @return property instance
   */
  <T> ObjectConfigProperty<T> objectProperty(Map<String, String> bindingMap, Class<T> cfgClass);

  /**
   * Returns current value of object property or defaults.
   *
   * @param cfgClass a class of config object instance
   * @param defaultValue default config object
   * @param <T> a type of returned config object
   * @return property value
   */
  <T> T objectValue(String prefix, Class<T> cfgClass, T defaultValue);

  /**
   * Returns current value of object property or defaults.
   *
   * @param bindingMap custom mapping between class field names and property names
   * @param cfgClass a class of config object instance
   * @param defaultValue default config object
   * @param <T> a type of returned config object
   * @return property value
   */
  <T> T objectValue(Map<String, String> bindingMap, Class<T> cfgClass, T defaultValue);

  /**
   * Returns dynamic typed string property. String property is a base type for all properties and
   * each type can be casted to string property.
   *
   * @param name property name
   * @return property instance
   */
  StringConfigProperty stringProperty(String name);

  /**
   * Returns current value of string property or defaults.
   *
   * @param name property name
   * @param defaultValue default property value
   * @return property value
   */
  String stringValue(String name, String defaultValue);

  /**
   * Returns dynamic typed double property.
   *
   * @param name property name
   * @return property instance
   */
  DoubleConfigProperty doubleProperty(String name);

  /**
   * Returns current value of double property or defaults.
   *
   * @param name property name
   * @param defaultValue default property value
   * @return property value
   */
  double doubleValue(String name, double defaultValue);

  /**
   * Returns dynamic typed long property.
   *
   * @param name property name
   * @return property instance
   */
  LongConfigProperty longProperty(String name);

  /**
   * Returns current value of long property or defaults.
   *
   * @param name property name
   * @param defaultValue default property value
   * @return property value
   */
  long longValue(String name, long defaultValue);

  /**
   * Returns dynamic typed boolean property.
   *
   * @param name property name
   * @return property instance
   */
  BooleanConfigProperty booleanProperty(String name);

  /**
   * Returns current value of boolean property or defaults.
   *
   * @param name property name
   * @param defaultValue default property value
   * @return property value
   */
  boolean booleanValue(String name, boolean defaultValue);

  /**
   * Returns dynamic typed integer property.
   *
   * @param name property name
   * @return property instance
   */
  IntConfigProperty intProperty(String name);

  /**
   * Returns current value of int property or defaults.
   *
   * @param name property name
   * @param defaultValue default property value
   * @return property value
   */
  int intValue(String name, int defaultValue);

  /**
   * Returns dynamic typed java8 duration property. See for details {@link
   * Duration#parse(java.lang.CharSequence)}.
   *
   * @param name property name
   * @return property instance
   */
  DurationConfigProperty durationProperty(String name);

  /**
   * Returns current value of java8 duration property or defaults. See for details {@link
   * Duration#parse(java.lang.CharSequence)}.
   *
   * @param name property name
   * @param defaultValue default property value
   * @return property value
   */
  Duration durationValue(String name, Duration defaultValue);

  /**
   * Returns dynamic generic-typed list property.
   *
   * @param name property name
   * @return property instance
   */
  ListConfigProperty<String> stringListProperty(String name);

  /**
   * Returns current value of list property or defaults.
   *
   * @param name property name
   * @param defaultValue default property value
   * @return property value
   */
  List<String> stringListValue(String name, List<String> defaultValue);

  /**
   * Returns dynamic generic-typed list property.
   *
   * @param name property name
   * @return property instance
   */
  ListConfigProperty<Double> doubleListProperty(String name);

  /**
   * Returns current value of list property or defaults.
   *
   * @param name property name
   * @param defaultValue default property value
   * @return property value
   */
  List<Double> doubleListValue(String name, List<Double> defaultValue);

  /**
   * Returns dynamic generic-typed list property.
   *
   * @param name property name
   * @return property instance
   */
  ListConfigProperty<Long> longListProperty(String name);

  /**
   * Returns current value of list property or defaults.
   *
   * @param name property name
   * @param defaultValue default property value
   * @return property value
   */
  List<Long> longListValue(String name, List<Long> defaultValue);

  /**
   * Returns dynamic generic-typed list property.
   *
   * @param name property name
   * @return property instance
   */
  ListConfigProperty<Integer> intListProperty(String name);

  /**
   * Returns current value of list property or defaults.
   *
   * @param name property name
   * @param defaultValue default property value
   * @return property value
   */
  List<Integer> intListValue(String name, List<Integer> defaultValue);

  /**
   * Returns dynamic generic-typed list property.
   *
   * @param name property name
   * @return property instance
   */
  ListConfigProperty<Duration> durationListProperty(String name);

  /**
   * Returns current value of list property or defaults.
   *
   * @param name property name
   * @param defaultValue default property value
   * @return property value
   */
  List<Duration> durationListValue(String name, List<Duration> defaultValue);

  /**
   * Returns dynamic generic-typed multimap property.
   *
   * @param name property name
   * @return property instance
   */
  MultimapConfigProperty<String> stringMultimapProperty(String name);

  /**
   * Returns current value of multimap property or defaults.
   *
   * @param name property name
   * @param defaultValue default property value
   * @return property value
   */
  Map<String, List<String>> stringMultimapValue(
      String name, Map<String, List<String>> defaultValue);

  /**
   * Returns dynamic generic-typed multimap property.
   *
   * @param name property name
   * @return property instance
   */
  MultimapConfigProperty<Double> doubleMultimapProperty(String name);

  /**
   * Returns current value of multimap property or defaults.
   *
   * @param name property name
   * @param defaultValue default property value
   * @return property value
   */
  Map<String, List<Double>> doubleMultimapValue(
      String name, Map<String, List<Double>> defaultValue);

  /**
   * Returns dynamic generic-typed multimap property.
   *
   * @param name property name
   * @return property instance
   */
  MultimapConfigProperty<Long> longMultimapProperty(String name);

  /**
   * Returns current value of multimap property or defaults.
   *
   * @param name property name
   * @param defaultValue default property value
   * @return property value
   */
  Map<String, List<Long>> longMultimapValue(String name, Map<String, List<Long>> defaultValue);

  /**
   * Returns dynamic generic-typed multimap property.
   *
   * @param name property name
   * @return property instance
   */
  MultimapConfigProperty<Integer> intMultimapProperty(String name);

  /**
   * Returns current value of multimap property or defaults.
   *
   * @param name property name
   * @param defaultValue default property value
   * @return property value
   */
  Map<String, List<Integer>> intMultimapValue(String name, Map<String, List<Integer>> defaultValue);

  /**
   * Returns dynamic generic-typed multimap property.
   *
   * @param name property name
   * @return property instance
   */
  MultimapConfigProperty<Duration> durationMultimapProperty(String name);

  /**
   * Returns current value of multimap property or defaults.
   *
   * @param name property name
   * @param defaultValue default property value
   * @return property value
   */
  Map<String, List<Duration>> durationMultimapValue(
      String name, Map<String, List<Duration>> defaultValue);

  /** Returns set of all loaded property keys. */
  Set<String> allProperties();

  /** Returns snapshot of all current property values. */
  Collection<ConfigPropertyInfo> getConfigProperties();

  /** Returns list of recent property changes events. */
  Collection<ConfigEvent> getRecentConfigEvents();

  /** Returns list of configured property sources descriptions. */
  Collection<ConfigSourceInfo> getConfigSources();

  /** Returns config registry settings. */
  ConfigRegistrySettings getSettings();
}
