package io.scalecube.config;

import io.scalecube.config.audit.ConfigEvent;
import io.scalecube.config.jmx.JmxConfigRegistry;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.ConfigSourceInfo;
import io.scalecube.config.source.LoadedConfigProperty;
import io.scalecube.config.utils.ThrowableUtil;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.management.MBeanServer;
import javax.management.ObjectName;

final class ConfigRegistryImpl implements ConfigRegistry {

  private static final Logger LOGGER = Logger.getLogger(ConfigRegistryImpl.class.getName());

  static final Function<String, String> STRING_PARSER = str -> str;
  static final Function<String, Double> DOUBLE_PARSER = Double::parseDouble;
  static final Function<String, Long> LONG_PARSER = Long::parseLong;
  static final Function<String, Boolean> BOOLEAN_PARSER = Boolean::parseBoolean;
  static final Function<String, Integer> INT_PARSER = Integer::parseInt;
  static final Function<String, Duration> DURATION_PARSER = DurationParser::parseDuration;

  // reload executor

  private static final ScheduledExecutorService reloadExecutor;

  static {
    ThreadFactory threadFactory =
        r -> {
          Thread thread = new Thread(r);
          thread.setDaemon(true);
          thread.setName("config-registry");
          thread.setUncaughtExceptionHandler(
              (t, e) -> LOGGER.log(Level.SEVERE, "Exception occurred: " + e, e));
          return thread;
        };
    reloadExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
  }

  // state fields

  private final ConfigRegistrySettings settings;

  private final Map<String, Integer> configSourceStatusMap = new HashMap<>();

  private volatile Map<String, LoadedConfigProperty> propertyMap; // being reset on reload

  @SuppressWarnings("rawtypes")
  private final Map<String, Map<Class, PropertyCallback>> propertyCallbackMap =
      new ConcurrentHashMap<>();

  private final LinkedHashMap<ConfigEvent, Object> recentConfigEvents =
      new LinkedHashMap<ConfigEvent, Object>() {
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

    if (settings.isReloadEnabled()) {
      reloadExecutor.scheduleAtFixedRate(
          () -> {
            try {
              loadAndNotify();
            } catch (Exception e) {
              LOGGER.log(Level.SEVERE, "[loadAndNotify] Exception occurred, cause: " + e);
            }
          },
          settings.getReloadIntervalSec(),
          settings.getReloadIntervalSec(),
          TimeUnit.SECONDS);
    }

    if (settings.isJmxEnabled()) {
      registerJmxMBean();
    }
  }

  private void registerJmxMBean() {
    try {
      MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
      ObjectName objectName = new ObjectName(settings.getJmxMBeanName());
      mbeanServer.registerMBean(new JmxConfigRegistry(this), objectName);
    } catch (Exception e) {
      throw ThrowableUtil.propagate(e);
    }
  }

  @Override
  public <T> ObjectConfigProperty<T> objectProperty(String name, Function<String, T> mapper) {
    return new MappedObjectConfigProperty<>(
        new StringConfigPropertyImpl(name, propertyMap, propertyCallbackMap), mapper);
  }

  @Override
  public <T> ObjectConfigProperty<T> objectProperty(String prefix, Class<T> cfgClass) {
    return objectProperty(
        Arrays.stream(cfgClass.getDeclaredFields())
            .collect(Collectors.toMap(Field::getName, field -> prefix + '.' + field.getName())),
        cfgClass);
  }

  @Override
  public <T> ObjectConfigProperty<T> objectProperty(
      Map<String, String> bindingMap, Class<T> cfgClass) {
    return new ObjectConfigPropertyImpl<>(bindingMap, cfgClass, propertyMap, propertyCallbackMap);
  }

  @Override
  public <T> ObjectConfigProperty<T> objectProperty(Class<T> cfgClass) {
    return objectProperty(cfgClass.getPackage().getName(), cfgClass);
  }

  @Override
  public <T> T objectValue(String prefix, Class<T> cfgClass, T defaultValue) {
    return objectProperty(prefix, cfgClass).value(defaultValue);
  }

  @Override
  public <T> T objectValue(Map<String, String> bindingMap, Class<T> cfgClass, T defaultValue) {
    return objectProperty(bindingMap, cfgClass).value(defaultValue);
  }

  @Override
  public <T> T objectValue(Class<T> cfgClass, T defaultValue) {
    return objectValue(cfgClass.getPackage().getName(), cfgClass, defaultValue);
  }

  @Override
  public StringConfigProperty stringProperty(String name) {
    return new StringConfigPropertyImpl(name, propertyMap, propertyCallbackMap);
  }

  @Override
  public String stringValue(String name, String defaultValue) {
    return stringProperty(name).value(defaultValue);
  }

  @Override
  public DoubleConfigProperty doubleProperty(String name) {
    return new DoubleConfigPropertyImpl(name, propertyMap, propertyCallbackMap);
  }

  @Override
  public double doubleValue(String name, double defaultValue) {
    return doubleProperty(name).value(defaultValue);
  }

  @Override
  public LongConfigProperty longProperty(String name) {
    return new LongConfigPropertyImpl(name, propertyMap, propertyCallbackMap);
  }

  @Override
  public long longValue(String name, long defaultValue) {
    return longProperty(name).value(defaultValue);
  }

  @Override
  public BooleanConfigProperty booleanProperty(String name) {
    return new BooleanConfigPropertyImpl(name, propertyMap, propertyCallbackMap);
  }

  @Override
  public boolean booleanValue(String name, boolean defaultValue) {
    return booleanProperty(name).value(defaultValue);
  }

  @Override
  public IntConfigProperty intProperty(String name) {
    return new IntConfigPropertyImpl(name, propertyMap, propertyCallbackMap);
  }

  @Override
  public int intValue(String name, int defaultValue) {
    return intProperty(name).value(defaultValue);
  }

  @Override
  public DurationConfigProperty durationProperty(String name) {
    return new DurationConfigPropertyImpl(name, propertyMap, propertyCallbackMap);
  }

  @Override
  public Duration durationValue(String name, Duration defaultValue) {
    return durationProperty(name).value(defaultValue);
  }

  @Override
  public ListConfigProperty<String> stringListProperty(String name) {
    return new ListConfigPropertyImpl<>(name, propertyMap, propertyCallbackMap, STRING_PARSER);
  }

  @Override
  public List<String> stringListValue(String name, List<String> defaultValue) {
    return stringListProperty(name).value(defaultValue);
  }

  @Override
  public ListConfigProperty<Double> doubleListProperty(String name) {
    return new ListConfigPropertyImpl<>(name, propertyMap, propertyCallbackMap, DOUBLE_PARSER);
  }

  @Override
  public List<Double> doubleListValue(String name, List<Double> defaultValue) {
    return doubleListProperty(name).value(defaultValue);
  }

  @Override
  public ListConfigProperty<Long> longListProperty(String name) {
    return new ListConfigPropertyImpl<>(name, propertyMap, propertyCallbackMap, LONG_PARSER);
  }

  @Override
  public List<Long> longListValue(String name, List<Long> defaultValue) {
    return longListProperty(name).value(defaultValue);
  }

  @Override
  public ListConfigProperty<Integer> intListProperty(String name) {
    return new ListConfigPropertyImpl<>(name, propertyMap, propertyCallbackMap, INT_PARSER);
  }

  @Override
  public List<Integer> intListValue(String name, List<Integer> defaultValue) {
    return intListProperty(name).value(defaultValue);
  }

  @Override
  public ListConfigProperty<Duration> durationListProperty(String name) {
    return new ListConfigPropertyImpl<>(name, propertyMap, propertyCallbackMap, DURATION_PARSER);
  }

  @Override
  public List<Duration> durationListValue(String name, List<Duration> defaultValue) {
    return durationListProperty(name).value(defaultValue);
  }

  @Override
  public MultimapConfigProperty<String> stringMultimapProperty(String name) {
    return new MultimapConfigPropertyImpl<>(name, propertyMap, propertyCallbackMap, STRING_PARSER);
  }

  @Override
  public Map<String, List<String>> stringMultimapValue(
      String name, Map<String, List<String>> defaultValue) {
    return stringMultimapProperty(name).value(defaultValue);
  }

  @Override
  public MultimapConfigProperty<Double> doubleMultimapProperty(String name) {
    return new MultimapConfigPropertyImpl<>(name, propertyMap, propertyCallbackMap, DOUBLE_PARSER);
  }

  @Override
  public Map<String, List<Double>> doubleMultimapValue(
      String name, Map<String, List<Double>> defaultValue) {
    return doubleMultimapProperty(name).value(defaultValue);
  }

  @Override
  public MultimapConfigProperty<Long> longMultimapProperty(String name) {
    return new MultimapConfigPropertyImpl<>(name, propertyMap, propertyCallbackMap, LONG_PARSER);
  }

  @Override
  public Map<String, List<Long>> longMultimapValue(
      String name, Map<String, List<Long>> defaultValue) {
    return longMultimapProperty(name).value(defaultValue);
  }

  @Override
  public MultimapConfigProperty<Integer> intMultimapProperty(String name) {
    return new MultimapConfigPropertyImpl<>(name, propertyMap, propertyCallbackMap, INT_PARSER);
  }

  @Override
  public Map<String, List<Integer>> intMultimapValue(
      String name, Map<String, List<Integer>> defaultValue) {
    return intMultimapProperty(name).value(defaultValue);
  }

  @Override
  public MultimapConfigProperty<Duration> durationMultimapProperty(String name) {
    return new MultimapConfigPropertyImpl<>(
        name, propertyMap, propertyCallbackMap, DURATION_PARSER);
  }

  @Override
  public Map<String, List<Duration>> durationMultimapValue(
      String name, Map<String, List<Duration>> defaultValue) {
    return durationMultimapProperty(name).value(defaultValue);
  }

  @Override
  public Set<String> allProperties() {
    return propertyMap.values().stream()
        .map(LoadedConfigProperty::name)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<ConfigPropertyInfo> getConfigProperties() {
    return propertyMap.values().stream()
        .map(
            property -> {
              ConfigPropertyInfo info = new ConfigPropertyInfo();
              info.setName(property.name());
              info.setValue(property.valueAsString().orElse(null));
              info.setSource(property.source().orElse(null));
              info.setOrigin(property.origin().orElse(null));
              info.setHost(settings.getHost());
              return info;
            })
        .collect(Collectors.toList());
  }

  @Override
  public Collection<ConfigSourceInfo> getConfigSources() {
    Collection<ConfigSourceInfo> result = new ArrayList<>();
    int order = 0;
    for (Map.Entry<String, ConfigSource> entry : settings.getSources().entrySet()) {
      int priorityOrder = order++;
      String sourceName = entry.getKey();
      ConfigSource configSource = entry.getValue();

      ConfigSourceInfo info = new ConfigSourceInfo();
      info.setSourceName(sourceName);
      info.setPriorityOrder(priorityOrder);
      info.setConfigSourceString(configSource.toString());

      Integer status = configSourceStatusMap.get(sourceName);
      info.setHealthString(
          Optional.ofNullable(status).map(i -> i == 1 ? "Error" : "Ok").orElse("Unknown"));

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
    Map<String, LoadedConfigProperty> loadedPropertyMap = new ConcurrentHashMap<>();

    // load config from sources
    Map<String, ConfigSource> sources = settings.getSources();
    for (String sourceName : sources.keySet()) {
      ConfigSource source = sources.get(sourceName);

      final Map<String, ConfigProperty> configMap;
      Throwable error = null;
      try {
        configMap = source.loadConfig();
      } catch (Exception e) {
        error = e;
        throw ThrowableUtil.propagate(e);
      } finally {
        computeConfigLoadStatus(sourceName, error);
      }

      // populate loaded properties with new field 'source'
      configMap.forEach(
          (key, configProperty) ->
              loadedPropertyMap.putIfAbsent(
                  key,
                  LoadedConfigProperty.withCopyFrom(configProperty).source(sourceName).build()));
    }

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

      Set<String> updatedProps =
          Stream.concat(keySet1.stream(), keySet2.stream())
              .filter(keySet1::contains)
              .filter(keySet2::contains)
              .collect(Collectors.toSet());

      for (String propName : updatedProps) {
        ConfigProperty newProp = loadedPropertyMap.get(propName); // not null
        ConfigProperty oldProp = propertyMap.get(propName); // not null
        // collect changes
        detectedChanges.add(
            ConfigEvent.createUpdated(propName, settings.getHost(), oldProp, newProp));
      }

      // Checks for removals
      Set<String> removedProps =
          keySet1.stream().filter(o -> !keySet2.contains(o)).collect(Collectors.toSet());
      for (String propName : removedProps) {
        ConfigProperty oldProp = propertyMap.get(propName);
        if (oldProp != null) {
          // collect changes
          detectedChanges.add(ConfigEvent.createRemoved(propName, settings.getHost(), oldProp));
        }
      }

      // Check for new properties
      Set<String> addedProps =
          keySet2.stream().filter(o -> !keySet1.contains(o)).collect(Collectors.toSet());
      for (String propName : addedProps) {
        ConfigProperty newProp = loadedPropertyMap.get(propName); // not null
        // collect changes
        detectedChanges.add(ConfigEvent.createAdded(propName, settings.getHost(), newProp));
      }
    }

    // reset loaded
    propertyMap = loadedPropertyMap;

    detectedChanges.forEach(input -> recentConfigEvents.put(input, null)); // keep recent changes

    reportChanges(
        detectedChanges.stream().filter(ConfigEvent::isChanged).collect(Collectors.toList()));

    // re-compute values and invoke callbacks
    detectedChanges.stream()
        .filter(event -> propertyCallbackMap.containsKey(event.getName()))
        .flatMap(
            event ->
                propertyCallbackMap.get(event.getName()).values().stream()
                    .map(callback -> new SimpleImmutableEntry<>(callback, event)))
        .collect(
            Collectors.groupingBy(
                SimpleImmutableEntry::getKey,
                Collectors.mapping(SimpleImmutableEntry::getValue, Collectors.toList())))
        .forEach(PropertyCallback::computeValue);
  }

  private void reportChanges(Collection<ConfigEvent> events) {
    Collection<ConfigEvent> configEvents = Collections.unmodifiableCollection(events);
    settings
        .getListeners()
        .forEach(
            (key, eventListener) -> {
              try {
                eventListener.onEvents(configEvents);
              } catch (Exception e) {
                LOGGER.log(
                    Level.SEVERE,
                    String.format(
                        "Exception on configEventListener: %s, events: %s", key, configEvents),
                    e);
              }
            });
  }

  private void computeConfigLoadStatus(String sourceName, Throwable ex) {
    int status = ex != null ? 1 : 0;
    Integer status0 = configSourceStatusMap.put(sourceName, status);
    if (status0 == null || (status0 ^ status) == 1) {
      if (status == 1) {
        LOGGER.log(
            Level.SEVERE, String.format("[loadConfig][%s] Exception occurred: %s", sourceName, ex));
      } else {
        LOGGER.log(
            Level.SEVERE, String.format("[loadConfig][%s] Loaded config properties", sourceName));
      }
    }
  }
}
