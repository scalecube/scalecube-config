package io.scalecube.config;

import io.scalecube.config.audit.ConfigEvent;
import io.scalecube.config.jmx.JmxConfigRegistry;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.ConfigSourceInfo;
import io.scalecube.config.source.LoadedConfigProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
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
import java.util.concurrent.ExecutorService;
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

  private final ConcurrentMap<String, BiConsumer> propertyCallbacks = new ConcurrentHashMap<>();

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
    StringConfigPropertyImpl configProperty = new StringConfigPropertyImpl();
    configProperty.configure(name, String.class);
    return configProperty;
  }

  @Override
  public String stringValue(String name, String defaultValue) {
    return stringProperty(name).get(defaultValue);
  }

  @Override
  public DoubleConfigProperty doubleProperty(String name) {
    DoubleConfigPropertyImpl configProperty = new DoubleConfigPropertyImpl();
    configProperty.configure(name, Double.class);
    return configProperty;
  }

  @Override
  public double doubleValue(String name, double defaultValue) {
    return doubleProperty(name).get(defaultValue);
  }

  @Override
  public LongConfigProperty longProperty(String name) {
    LongConfigPropertyImpl configProperty = new LongConfigPropertyImpl();
    configProperty.configure(name, Long.class);
    return configProperty;
  }

  @Override
  public long longValue(String name, long defaultValue) {
    return longProperty(name).get(defaultValue);
  }

  @Override
  public BooleanConfigProperty booleanProperty(String name) {
    BooleanConfigPropertyImpl configProperty = new BooleanConfigPropertyImpl();
    configProperty.configure(name, Boolean.class);
    return configProperty;
  }

  @Override
  public boolean booleanValue(String name, boolean defaultValue) {
    return booleanProperty(name).get(defaultValue);
  }

  @Override
  public IntConfigProperty intProperty(String name) {
    IntConfigPropertyImpl configProperty = new IntConfigPropertyImpl();
    configProperty.configure(name, Integer.class);
    return configProperty;
  }

  @Override
  public int intValue(String name, int defaultValue) {
    return intProperty(name).get(defaultValue);
  }

  @Override
  public Set<String> allProperties() {
    return propertyMap.values().stream().map(ConfigProperty::getName).collect(Collectors.toSet());
  }

  @Override
  public Collection<ConfigPropertyInfo> getConfigProperties() {
    // noinspection RedundantCast
    return propertyMap.values().stream().map((Function<ConfigProperty, ConfigPropertyInfo>) property -> {
      ConfigPropertyInfo info = new ConfigPropertyInfo();
      info.setName(property.getName());
      info.setValue(property.getAsString().orElse(null));
      info.setSource(property.getSource().orElse(null));
      info.setOrigin(property.getOrigin().orElse(null));
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
        if (!oldProp.getAsString().equals(newProp.getAsString())) {
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
    BiConsumer callback = propertyCallbacks.get(event.getName());
    if (callback != null) {
      try {
        // noinspection unchecked
        callback.accept(event.getOldValue(), event.getNewValue());
      } catch (Exception e) {
        LOGGER.error("Exception occurred on property-change callback: {}, event: {}, cause: {}",
            callback, event, e, e);
      }
    }
  }

  private abstract class AbstractConfigProperty<T> implements ConfigProperty {
    private String name;
    private Function<String, Object> valueParser;

    final void configure(String name, Class type) {
      this.name = name;
      // configure value parser
      if (type == Boolean.class) {
        valueParser = Boolean::parseBoolean;
      } else if (type == Long.class) {
        valueParser = Long::parseLong;
      } else if (type == String.class) {
        valueParser = str -> str;
      } else if (type == Double.class) {
        valueParser = Double::parseDouble;
      } else if (type == Integer.class) {
        valueParser = Integer::parseInt;
      } else {
        throw new IllegalArgumentException("Unsupported property type: " + type);
      }
    }

    @Override
    public final String getName() {
      return name;
    }

    @Override
    public final Optional<String> getSource() {
      return Optional.ofNullable(propertyMap.get(name)).flatMap(ConfigProperty::getSource);
    }

    @Override
    public final Optional<String> getOrigin() {
      return Optional.ofNullable(propertyMap.get(name)).flatMap(ConfigProperty::getOrigin);
    }

    @Override
    public final Optional<String> getAsString() {
      return Optional.ofNullable(propertyMap.get(name)).flatMap(ConfigProperty::getAsString);
    }

    @Override
    public final String getAsString(String defaultValue) {
      return getAsString().orElse(defaultValue);
    }

    public final Optional<T> get() {
      // noinspection unchecked
      return getAsString().map(str -> (T) valueParser.apply(str));
    }

    // used by subclass
    public final void setCallback(BiConsumer<T, T> callback) {
      BiConsumer<String, String> callback1 = (oldValue, newValue) -> {
        // noinspection unchecked
        T oldValue1 = oldValue != null ? (T) valueParser.apply(oldValue) : null;
        // noinspection unchecked
        T newValue1 = newValue != null ? (T) valueParser.apply(newValue) : null;
        callback.accept(oldValue1, newValue1);
      };
      propertyCallbacks.put(name, callback1);
    }

    // used by subclass
    public final void setCallback(ExecutorService executor, BiConsumer<T, T> callback) {
      BiConsumer<String, String> callback1 = (oldValue, newValue) -> {
        // noinspection unchecked
        T oldValue1 = oldValue != null ? (T) valueParser.apply(oldValue) : null;
        // noinspection unchecked
        T newValue1 = newValue != null ? (T) valueParser.apply(newValue) : null;
        executor.execute(() -> callback.accept(oldValue1, newValue1));
      };
      propertyCallbacks.put(name, callback1);
    }
  }

  private class DoubleConfigPropertyImpl extends AbstractConfigProperty<Double> implements DoubleConfigProperty {
    @Override
    public double get(double defaultValue) {
      return get().orElse(defaultValue);
    }
  }

  private class LongConfigPropertyImpl extends AbstractConfigProperty<Long> implements LongConfigProperty {
    @Override
    public long get(long defaultValue) {
      return get().orElse(defaultValue);
    }
  }

  private class BooleanConfigPropertyImpl extends AbstractConfigProperty<Boolean> implements BooleanConfigProperty {
    @Override
    public boolean get(boolean defaultValue) {
      return get().orElse(defaultValue);
    }
  }

  private class IntConfigPropertyImpl extends AbstractConfigProperty<Integer> implements IntConfigProperty {
    @Override
    public int get(int defaultValue) {
      return get().orElse(defaultValue);
    }
  }

  private class StringConfigPropertyImpl extends AbstractConfigProperty<String> implements StringConfigProperty {
    @Override
    public String get(String defaultValue) {
      return super.getAsString(defaultValue);
    }
  }
}
