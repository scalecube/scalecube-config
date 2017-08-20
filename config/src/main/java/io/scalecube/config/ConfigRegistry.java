package io.scalecube.config;

import io.scalecube.config.audit.ConfigEvent;
import io.scalecube.config.source.ConfigSourceInfo;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
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
   * Returns dynamic typed string property. String property is a base type for all properties and each type can be
   * casted to string property.
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
   * Returns dynamic typed java8 duration property. See for details {@link Duration#parse(java.lang.CharSequence)}.
   *
   * @param name property name
   * @return property instance
   */
  DurationConfigProperty durationProperty(String name);

  /**
   * Returns current value of java8 duration property or defaults. See for details
   * {@link Duration#parse(java.lang.CharSequence)}.
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
   * Returns set of all loaded property keys.
   */
  Set<String> allProperties();

  /**
   * Returns snapshot of all current property values.
   */
  Collection<ConfigPropertyInfo> getConfigProperties();

  /**
   * Returns list of recent property changes events.
   */
  Collection<ConfigEvent> getRecentConfigEvents();

  /**
   * Returns list of configured property sources descriptions.
   */
  Collection<ConfigSourceInfo> getConfigSources();

  /**
   * Returns config registry settings.
   */
  ConfigRegistrySettings getSettings();

}
