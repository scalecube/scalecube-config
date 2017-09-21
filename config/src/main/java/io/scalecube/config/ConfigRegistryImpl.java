package io.scalecube.config;

import io.scalecube.config.audit.ConfigEvent;
import io.scalecube.config.jmx.JmxConfigRegistry;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.ConfigSourceInfo;
import io.scalecube.config.source.LoadedConfigProperty;
import io.scalecube.config.utils.ThrowableUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

final class ConfigRegistryImpl implements ConfigRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRegistryImpl.class);

  // reload executor

  private static final ScheduledExecutorService reloadExecutor;
  static {
    ThreadFactory threadFactory = r -> {
      Thread thread = new Thread(r);
      thread.setDaemon(true);
      thread.setName("config-reloader");
      thread.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Exception occurred: " + e, e));
      return thread;
    };
    reloadExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
  }

  // state fields

  private final ConfigRegistrySettings settings;

  private final Map<String, Throwable> configSourceHealthMap = new HashMap<>();

  private volatile Map<String, ConfigProperty> propertyMap;

  private final ConcurrentMap<String, PropertyCallback> propertyCallbacks = new ConcurrentHashMap<>();

  private final LinkedHashMap<ConfigEvent, Object> recentConfigEvents = new LinkedHashMap<ConfigEvent, Object>() {
    @Override
    protected boolean removeEldestEntry(Map.Entry<ConfigEvent, Object> eldest) {
      return size() > settings.getRecentConfigEventsNum();
    }
  };

  ConfigRegistryImpl(ConfigRegistrySettings settings) {
    Objects.requireNonNull(settings, "ConfigRegistrySettings can't be null");
    this.settings = settings;
  }

  void init() {
    loadAndNotify();
    reloadExecutor.scheduleAtFixedRate(() -> {
      try {
        loadAndNotify();
      } catch (Exception e) {
        LOGGER.error("Exception on config reload, cause: {}", e, e);
      }
    }, settings.getReloadIntervalSec(), settings.getReloadIntervalSec(), TimeUnit.SECONDS);

    if (settings.isJmxEnabled()) {
      registerJmxMBean();
    }
  }

  private void registerJmxMBean() {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ObjectName objectName = new ObjectName(settings.getJmxMBeanName());
      mBeanServer.registerMBean(new JmxConfigRegistry(this), objectName);
      MBeanInfo mBeanInfo = mBeanServer.getMBeanInfo(objectName);
      LOGGER.info("Registered JMX MBean: {}", mBeanInfo);
    } catch (Exception e) {
      LOGGER.warn("Failed to register JMX MBean '{}', cause: {}", settings.getJmxMBeanName(), e);
    }
  }

  @Override
  public <T> ObjectConfigProperty<T> objectProperty(String prefix, Class<T> objClass) {
    Map<String, String> bindingMap = Arrays.stream(objClass.getDeclaredFields())
        .collect(Collectors.toMap(Field::getName, field -> prefix + '.' + field.getName()));
    return new ObjectConfigPropertyImpl<>(bindingMap, objClass);
  }

  @Override
  public <T> ObjectConfigProperty<T> objectProperty(Map<String, String> bindingMap, Class<T> objClass) {
    return new ObjectConfigPropertyImpl<>(bindingMap, objClass);
  }

  @Override
  public <T> T objectValue(String prefix, Class<T> objClass, T defaultValue) {
    return objectProperty(prefix, objClass).value(defaultValue);
  }

  @Override
  public <T> T objectValue(Map<String, String> bindingMap, Class<T> objClass, T defaultValue) {
    return objectProperty(bindingMap, objClass).value(defaultValue);
  }

  @Override
  public StringConfigProperty stringProperty(String name) {
    return new StringConfigPropertyImpl(name);
  }

  @Override
  public String stringValue(String name, String defaultValue) {
    return stringProperty(name).value(defaultValue);
  }

  @Override
  public DoubleConfigProperty doubleProperty(String name) {
    return new DoubleConfigPropertyImpl(name);
  }

  @Override
  public double doubleValue(String name, double defaultValue) {
    return doubleProperty(name).value(defaultValue);
  }

  @Override
  public LongConfigProperty longProperty(String name) {
    return new LongConfigPropertyImpl(name);
  }

  @Override
  public long longValue(String name, long defaultValue) {
    return longProperty(name).value(defaultValue);
  }

  @Override
  public BooleanConfigProperty booleanProperty(String name) {
    return new BooleanConfigPropertyImpl(name);
  }

  @Override
  public boolean booleanValue(String name, boolean defaultValue) {
    return booleanProperty(name).value(defaultValue);
  }

  @Override
  public IntConfigProperty intProperty(String name) {
    return new IntConfigPropertyImpl(name);
  }

  @Override
  public int intValue(String name, int defaultValue) {
    return intProperty(name).value(defaultValue);
  }

  @Override
  public DurationConfigProperty durationProperty(String name) {
    return new DurationConfigPropertyImpl(name);
  }

  @Override
  public Duration durationValue(String name, Duration defaultValue) {
    return durationProperty(name).value(defaultValue);
  }

  @Override
  public ListConfigProperty<String> stringListProperty(String name) {
    return new ListConfigPropertyImpl<>(name, str -> str);
  }

  @Override
  public List<String> stringListValue(String name, List<String> defaultValue) {
    return stringListProperty(name).value(defaultValue);
  }

  @Override
  public ListConfigProperty<Double> doubleListProperty(String name) {
    return new ListConfigPropertyImpl<>(name, Double::parseDouble);
  }

  @Override
  public List<Double> doubleListValue(String name, List<Double> defaultValue) {
    return doubleListProperty(name).value(defaultValue);
  }

  @Override
  public ListConfigProperty<Long> longListProperty(String name) {
    return new ListConfigPropertyImpl<>(name, Long::parseLong);
  }

  @Override
  public List<Long> longListValue(String name, List<Long> defaultValue) {
    return longListProperty(name).value(defaultValue);
  }

  @Override
  public ListConfigProperty<Integer> intListProperty(String name) {
    return new ListConfigPropertyImpl<>(name, Integer::parseInt);
  }

  @Override
  public List<Integer> intListValue(String name, List<Integer> defaultValue) {
    return intListProperty(name).value(defaultValue);
  }

  @Override
  public Set<String> allProperties() {
    return propertyMap.values().stream().map(ConfigProperty::name).collect(Collectors.toSet());
  }

  @Override
  public Collection<ConfigPropertyInfo> getConfigProperties() {
    return propertyMap.values().stream().map(property -> {
      ConfigPropertyInfo info = new ConfigPropertyInfo();
      info.setName(property.name());
      info.setValue(property.valueAsString().orElse(null));
      info.setSource(property.source().orElse(null));
      info.setOrigin(property.origin().orElse(null));
      info.setHost(settings.getHost());
      return info;
    }).collect(Collectors.toList());
  }

  @Override
  public Collection<ConfigSourceInfo> getConfigSources() {
    Collection<ConfigSourceInfo> result = new ArrayList<>();
    int order = 0;
    for (Map.Entry<String, ConfigSource> entry : settings.getSources().entrySet()) {
      int priorityOrder = order++;
      String configSourceName = entry.getKey();
      ConfigSource configSource = entry.getValue();

      ConfigSourceInfo info = new ConfigSourceInfo();
      info.setSourceName(configSourceName);
      info.setPriorityOrder(priorityOrder);
      info.setConfigSourceString(configSource.toString());

      // noinspection ThrowableResultOfMethodCallIgnored
      Throwable throwable = configSourceHealthMap.get(configSourceName);
      info.setHealthString(throwable != null ? "error: " + throwable.toString() : "ok");

      info.setHost(settings.getHost());
      result.add(info);
    }
    return result;
  }

  @Override
  public Collection<ConfigEvent> getRecentConfigEvents() {
    return new LinkedHashSet<>(recentConfigEvents.keySet());
  }

  @Override
  public ConfigRegistrySettings getSettings() {
    return settings;
  }

  private void loadAndNotify() {
    // calculate new load map
    Map<String, ConfigProperty> loadedPropertyMap = new ConcurrentHashMap<>();

    settings.getSources().forEach((source, configSource) -> {
      Map<String, ConfigProperty> configMap = null;
      Throwable configError = null;
      try {
        configMap = configSource.loadConfig();
      } catch (ConfigSourceNotAvailableException e) {
        configError = e; // save error occurence
        LOGGER.warn("ConfigSource: {} failed on loadConfig, cause: {}", configSource, e);
      } catch (Throwable throwable) {
        configError = throwable; // save error occurence
        LOGGER.error("Exception on loading config from configSource: {}, source: {}, cause: {}",
            configSource, source, throwable, throwable);
      }

      // noinspection ThrowableResultOfMethodCallIgnored
      configSourceHealthMap.put(source, configError);

      if (configError != null) {
        return;
      }

      // populate loaded properties with new field -- source
      configMap.forEach((key, configProperty) -> loadedPropertyMap.putIfAbsent(key,
          LoadedConfigProperty.withCopyFrom(configProperty).source(source).build()));
    });

    List<ConfigEvent> detectedChanges = new ArrayList<>();

    if (propertyMap == null) {
      for (String propName : loadedPropertyMap.keySet()) {
        ConfigProperty newProp = loadedPropertyMap.get(propName); // not null
        // collect changes
        detectedChanges.add(ConfigEvent.createAdded(propName, settings.getHost(), newProp));
      }
    } else {
      // Check property updates
      Set<String> keySet1 = propertyMap.keySet();
      Set<String> keySet2 = loadedPropertyMap.keySet();

      Set<String> updatedProps = Stream.concat(keySet1.stream(), keySet2.stream())
          .filter(keySet1::contains)
          .filter(keySet2::contains)
          .collect(Collectors.toSet());

      for (String propName : updatedProps) {
        ConfigProperty newProp = loadedPropertyMap.get(propName); // not null
        ConfigProperty oldProp = propertyMap.get(propName); // not null
        // collect changes
        if (!oldProp.valueAsString().equals(newProp.valueAsString())) {
          detectedChanges.add(ConfigEvent.createUpdated(propName, settings.getHost(), oldProp, newProp));
        }
      }

      // Checks for removals
      Set<String> removedProps = keySet1.stream().filter(o -> !keySet2.contains(o)).collect(Collectors.toSet());
      for (String propName : removedProps) {
        ConfigProperty oldProp = propertyMap.get(propName);
        if (oldProp != null) {
          // collect changes
          detectedChanges.add(ConfigEvent.createRemoved(propName, settings.getHost(), oldProp));
        }
      }

      // Check for new properties
      Set<String> addedProps = keySet2.stream().filter(o -> !keySet1.contains(o)).collect(Collectors.toSet());
      for (String propName : addedProps) {
        ConfigProperty newProp = loadedPropertyMap.get(propName); // not null
        // collect changes
        detectedChanges.add(ConfigEvent.createAdded(propName, settings.getHost(), newProp));
      }
    }

    // reset loaded
    propertyMap = loadedPropertyMap;

    detectedChanges.forEach(input -> recentConfigEvents.put(input, null)); // keep recent changes
    detectedChanges.forEach(this::reportChanges); // report changes

    // invoke callbacks on changed values
    detectedChanges.stream()
        .filter(configEvent -> propertyCallbacks.containsKey(configEvent.getName()))
        .collect(Collectors.groupingBy(configEvent -> propertyCallbacks.get(configEvent.getName())))
        .forEach((propertyCallback, configEvents) -> {
          List<PropertyNameAndValue> oldList = new ArrayList<>();
          List<PropertyNameAndValue> newList = new ArrayList<>();

          for (ConfigEvent configEvent : configEvents) {
            oldList.add(new PropertyNameAndValue(configEvent.getName(), configEvent.getOldValue()));
            newList.add(new PropertyNameAndValue(configEvent.getName(), configEvent.getNewValue()));
          }

          propertyCallback.accept(oldList, newList);
        });
  }

  private void reportChanges(ConfigEvent event) {
    settings.getListeners().forEach((key, eventListener) -> {
      try {
        eventListener.onEvent(event);
      } catch (Exception e) {
        LOGGER.error("Exception on configEventListener: {}, event: {}, cause: {}", key, event, e, e);
      }
    });
  }

  private abstract class AbstractConfigProperty<T> implements ConfigProperty {
    private final String name;
    private final Function<List<PropertyNameAndValue>, T> valueParser;
    private final PropertyCallback<T> propertyCallback;

    AbstractConfigProperty(String name, Function<List<PropertyNameAndValue>, T> valueParser) {
      this.name = name;
      this.valueParser = valueParser;
      this.propertyCallback = new PropertyCallback<>(valueParser);
    }

    @Override
    public final String name() {
      return name;
    }

    @Override
    public final Optional<String> source() {
      return Optional.ofNullable(propertyMap.get(name)).flatMap(ConfigProperty::source);
    }

    @Override
    public final Optional<String> origin() {
      return Optional.ofNullable(propertyMap.get(name)).flatMap(ConfigProperty::origin);
    }

    @Override
    public final Optional<String> valueAsString() {
      return Optional.ofNullable(propertyMap.get(name)).flatMap(ConfigProperty::valueAsString);
    }

    @Override
    public final String valueAsString(String defaultValue) {
      return valueAsString().orElse(defaultValue);
    }

    public final Optional<T> value() {
      return valueAsString().flatMap(value -> {
        try {
          List<PropertyNameAndValue> nameValueList = Collections.singletonList(new PropertyNameAndValue(name, value));
          // noinspection unchecked
          return Optional.ofNullable(valueParser.apply(nameValueList));
        } catch (Exception e) {
          LOGGER.error("Exception at valueParser on property: '{}', string value: '{}', cause: {}", name, value, e);
          return Optional.empty();
        }
      });
    }

    final NoSuchElementException newValueIsNullException() {
      return new NoSuchElementException("Value is null for property '" + name + "'");
    }

    // used by subclass
    public final void addCallback(BiConsumer<T, T> callback) {
      propertyCallback.addCallback(callback);
      propertyCallbacks.putIfAbsent(name, propertyCallback);
    }

    // used by subclass
    public final void addCallback(Executor executor, BiConsumer<T, T> callback) {
      propertyCallback.addCallback(executor, callback);
      propertyCallbacks.putIfAbsent(name, propertyCallback);
    }
  }

  private class DoubleConfigPropertyImpl extends AbstractConfigProperty<Double> implements DoubleConfigProperty {

    DoubleConfigPropertyImpl(String name) {
      super(name, list -> list.get(0).getValue().map(Double::parseDouble).orElse(null));
    }

    @Override
    public double value(double defaultValue) {
      return value().orElse(defaultValue);
    }

    @Override
    public double valueOrThrow() {
      return value().orElseThrow(this::newValueIsNullException);
    }
  }

  private class LongConfigPropertyImpl extends AbstractConfigProperty<Long> implements LongConfigProperty {

    LongConfigPropertyImpl(String name) {
      super(name, list -> list.get(0).getValue().map(Long::parseLong).orElse(null));
    }

    @Override
    public long value(long defaultValue) {
      return value().orElse(defaultValue);
    }

    @Override
    public long valueOrThrow() {
      return value().orElseThrow(this::newValueIsNullException);
    }
  }

  private class BooleanConfigPropertyImpl extends AbstractConfigProperty<Boolean> implements BooleanConfigProperty {

    BooleanConfigPropertyImpl(String name) {
      super(name, list -> list.get(0).getValue().map(Boolean::parseBoolean).orElse(null));
    }

    @Override
    public boolean value(boolean defaultValue) {
      return value().orElse(defaultValue);
    }

    @Override
    public boolean valueOrThrow() {
      return value().orElseThrow(this::newValueIsNullException);
    }
  }

  private class IntConfigPropertyImpl extends AbstractConfigProperty<Integer> implements IntConfigProperty {

    IntConfigPropertyImpl(String name) {
      super(name, list -> list.get(0).getValue().map(Integer::parseInt).orElse(null));
    }

    @Override
    public int value(int defaultValue) {
      return value().orElse(defaultValue);
    }

    @Override
    public int valueOrThrow() {
      return value().orElseThrow(this::newValueIsNullException);
    }
  }

  private class DurationConfigPropertyImpl extends AbstractConfigProperty<Duration> implements DurationConfigProperty {

    DurationConfigPropertyImpl(String name) {
      super(name, list -> list.get(0).getValue().map(DurationParser::parse).orElse(null));
    }

    @Override
    public Duration value(Duration defaultValue) {
      return value().orElse(defaultValue);
    }

    @Override
    public Duration valueOrThrow() {
      return value().orElseThrow(this::newValueIsNullException);
    }
  }

  private class ListConfigPropertyImpl<T> extends AbstractConfigProperty<List<T>> implements ListConfigProperty<T> {

    ListConfigPropertyImpl(String name, Function<String, T> valueParser) {
      super(name, list -> list.get(0).getValue()
          .map(str -> Arrays.stream(str.split(",")).map(valueParser).collect(Collectors.toList()))
          .orElse(null));
    }

    @Override
    public List<T> value(List<T> defaultValue) {
      return value().orElse(defaultValue);
    }

    @Override
    public List<T> valueOrThrow() {
      return value().orElseThrow(this::newValueIsNullException);
    }
  }

  private class StringConfigPropertyImpl extends AbstractConfigProperty<String> implements StringConfigProperty {

    StringConfigPropertyImpl(String name) {
      super(name, list -> list.get(0).getValue().orElse(null));
    }

    @Override
    public String value(String defaultValue) {
      return super.valueAsString(defaultValue);
    }

    @Override
    public String valueOrThrow() {
      return value().orElseThrow(this::newValueIsNullException);
    }
  }

  private class ObjectConfigPropertyImpl<T> implements ObjectConfigProperty<T> {
    private final List<ObjectPropertyField> fields;
    private final Class<T> objClass;
    private final Function<List<PropertyNameAndValue>, T> valueParser;
    private final PropertyCallback<T> propertyCallback;

    ObjectConfigPropertyImpl(Map<String, String> bindingMap, Class<T> objClass) {
      this.objClass = objClass;

      // populate and prepare fields map
      fields = new ArrayList<>(bindingMap.size());
      for (String fieldName : bindingMap.keySet()) {
        Field field;
        try {
          field = objClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
          throw ThrowableUtil.propagate(e);
        }
        int modifiers = field.getModifiers();
        if (!Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
          fields.add(new ObjectPropertyField(field, bindingMap.get(fieldName)));
        }
      }

      this.valueParser = list -> ObjectPropertyParser.parse(list, fields, objClass);
      this.propertyCallback = new PropertyCallback<>(valueParser);
    }

    @Override
    public String name() {
      return objClass.getName();
    }

    @Override
    public Optional<T> value() {
      Map<String, ConfigProperty> propertyMap = ConfigRegistryImpl.this.propertyMap; // save the ref to temp variable

      List<PropertyNameAndValue> nameValueList = fields.stream()
          .map(ObjectPropertyField::getPropertyName)
          .filter(propertyMap::containsKey)
          .map(propertyMap::get)
          .map(configProperty -> new PropertyNameAndValue(configProperty.name(), configProperty.valueAsString(null)))
          .collect(Collectors.toList());

      try {
        // noinspection unchecked
        return Optional.ofNullable(valueParser.apply(nameValueList));
      } catch (Exception e) {
        LOGGER.error("Exception at valueParser on objectProperty: '{}', cause: {}", name(), e);
        return Optional.empty();
      }
    }

    @Override
    public T value(T defaultValue) {
      return value().orElse(defaultValue);
    }

    public void addCallback(BiConsumer<T, T> callback) {
      propertyCallback.addCallback(callback);
      fields.stream()
          .map(ObjectPropertyField::getPropertyName)
          .forEach(propName -> propertyCallbacks.computeIfAbsent(propName, name -> propertyCallback));
    }

    public void addCallback(Executor executor, BiConsumer<T, T> callback) {
      propertyCallback.addCallback(executor, callback);
      fields.stream()
          .map(ObjectPropertyField::getPropertyName)
          .forEach(propName -> propertyCallbacks.computeIfAbsent(propName, name -> propertyCallback));
    }
  }
}
