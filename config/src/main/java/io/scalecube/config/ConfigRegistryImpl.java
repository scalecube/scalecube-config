package io.scalecube.config;

import io.scalecube.config.audit.ConfigEvent;
import io.scalecube.config.jmx.JmxConfigRegistry;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.ConfigSourceInfo;
import io.scalecube.config.source.LoadedConfigProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

import javax.annotation.PostConstruct;
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

  @PostConstruct
  void init() {
    loadAndNotify();
    reloadExecutor.scheduleAtFixedRate(() -> {
      try {
        loadAndNotify();
      } catch (Exception e) {
        LOGGER.error("Exception occurred on config reload, cause: {}", e, e);
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
      Throwable loadConfigError = null;
      try {
        configMap = configSource.loadConfig();
      } catch (Throwable throwable) {
        LOGGER.error("Exception occurred on loading config from configSource: {}, source: {}, cause: {}",
            configSource, source, throwable, throwable);
        loadConfigError = throwable;
      }

      // noinspection ThrowableResultOfMethodCallIgnored
      configSourceHealthMap.put(source, loadConfigError);

      if (loadConfigError != null) {
        return;
      }

      // populate loaded properties with new field -- source
      configMap.forEach((key, value) -> loadedPropertyMap.putIfAbsent(key,
          LoadedConfigProperty.withCopyFrom(value).source(source).build()));
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
    detectedChanges.forEach(this::invokeCallbacks); // invoke callbacks on changed values
  }

  private void reportChanges(ConfigEvent event) {
    settings.getListeners().forEach((key, value) -> {
      try {
        value.onEvent(event);
      } catch (Exception e) {
        LOGGER.error("Exception occurred on configEventListener: {}, event: {}, cause: {}", key, event, e, e);
      }
    });
  }

  private void invokeCallbacks(ConfigEvent event) {
    PropertyCallback propertyCallback = propertyCallbacks.get(event.getName());
    if (propertyCallback != null) {
      propertyCallback.accept(event.getOldValue(), event.getNewValue());
    }
  }

  private abstract class AbstractConfigProperty<T> implements ConfigProperty {
    private final String name;
    private final Function<String, Object> valueParser;

    AbstractConfigProperty(String name, Function<String, Object> valueParser) {
      this.name = name;
      this.valueParser = valueParser;
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
      // noinspection unchecked
      return valueAsString().map(str -> (T) valueParser.apply(str));
    }

    // used by subclass
    public final void addCallback(BiConsumer<T, T> callback) {
      propertyCallbacks.computeIfAbsent(name, name -> new PropertyCallback<T>(valueParser));
      // noinspection unchecked
      propertyCallbacks.get(name).addCallback(callback);
    }

    // used by subclass
    public final void addCallback(Executor executor, BiConsumer<T, T> callback) {
      propertyCallbacks.computeIfAbsent(name, name -> new PropertyCallback<T>(valueParser));
      // noinspection unchecked
      propertyCallbacks.get(name).addCallback(executor, callback);
    }
  }

  private class DoubleConfigPropertyImpl extends AbstractConfigProperty<Double> implements DoubleConfigProperty {

    DoubleConfigPropertyImpl(String name) {
      super(name, Double::parseDouble);
    }

    @Override
    public double value(double defaultValue) {
      return value().orElse(defaultValue);
    }
  }

  private class LongConfigPropertyImpl extends AbstractConfigProperty<Long> implements LongConfigProperty {

    LongConfigPropertyImpl(String name) {
      super(name, Long::parseLong);
    }

    @Override
    public long value(long defaultValue) {
      return value().orElse(defaultValue);
    }
  }

  private class BooleanConfigPropertyImpl extends AbstractConfigProperty<Boolean> implements BooleanConfigProperty {

    BooleanConfigPropertyImpl(String name) {
      super(name, Boolean::new);
    }

    @Override
    public boolean value(boolean defaultValue) {
      return value().orElse(defaultValue);
    }
  }

  private class IntConfigPropertyImpl extends AbstractConfigProperty<Integer> implements IntConfigProperty {

    IntConfigPropertyImpl(String name) {
      super(name, Integer::parseInt);
    }

    @Override
    public int value(int defaultValue) {
      return value().orElse(defaultValue);
    }
  }

  private class DurationConfigPropertyImpl extends AbstractConfigProperty<Duration> implements DurationConfigProperty {

    DurationConfigPropertyImpl(String name) {
      super(name, Duration::parse);
    }

    @Override
    public Duration value(Duration defaultValue) {
      return value().orElse(defaultValue);
    }
  }

  private class ListConfigPropertyImpl<T> extends AbstractConfigProperty<List<T>> implements ListConfigProperty<T> {

    ListConfigPropertyImpl(String name, Function<String, Object> valueParser) {
      super(name, str -> Arrays.stream(str.split(",")).map(valueParser).collect(Collectors.toList()));
    }

    @Override
    public List<T> value(List<T> defaultValue) {
      return value().orElse(defaultValue);
    }
  }

  private class StringConfigPropertyImpl extends AbstractConfigProperty<String> implements StringConfigProperty {

    StringConfigPropertyImpl(String name) {
      super(name, str -> str);
    }

    @Override
    public String value(String defaultValue) {
      return super.valueAsString(defaultValue);
    }
  }
}
