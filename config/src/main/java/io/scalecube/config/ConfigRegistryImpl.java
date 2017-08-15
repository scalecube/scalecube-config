package io.scalecube.config;

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
import java.util.concurrent.CopyOnWriteArrayList;
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

import io.scalecube.config.audit.ConfigEvent;
import io.scalecube.config.jmx.JmxConfigRegistry;
import io.scalecube.config.source.ConfigSourceInfo;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.LoadedConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private final ConcurrentMap<String, Collection<BiConsumer>> propertyCallbacks = new ConcurrentHashMap<>();

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
	public DoubleConfigProperty doubleProperty(String name) {
		return new DoubleConfigPropertyImpl(name);
	}

	@Override
	public LongConfigProperty longProperty(String name) {
		return new LongConfigPropertyImpl(name);
	}

	@Override
	public BooleanConfigProperty booleanProperty(String name) {
		return new BooleanConfigPropertyImpl(name);
	}

	@Override
	public IntConfigProperty intProperty(String name) {
		return new IntConfigPropertyImpl(name);
	}

	@Override
	public Set<String> allProperties() {
		return propertyMap.values().stream()
				.map(ConfigProperty::getName)
				.collect(Collectors.toSet());
	}

	@Override
	public Collection<ConfigPropertyInfo> getConfigProperties() {
		return propertyMap.values().stream().map(property -> {
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

			//noinspection ThrowableResultOfMethodCallIgnored
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

		settings.getSources().entrySet().forEach(entry -> {
			String source = entry.getKey();
			ConfigSource configSource = entry.getValue();

			Map<String, ConfigProperty> configMap = null;
			Throwable loadConfigError = null;
			try {
				configMap = configSource.loadConfig();
			} catch (Throwable throwable) {
				LOGGER.error("Exception occurred on loading config from configSource: {}, source: {}, cause: {}",
						configSource, source, throwable, throwable);
				loadConfigError = throwable;
			}

			//noinspection ThrowableResultOfMethodCallIgnored
			configSourceHealthMap.put(source, loadConfigError);

			if (loadConfigError != null) {
				return;
			}

			// populate loaded properties with new field -- source
			configMap.entrySet().forEach(entry1 -> loadedPropertyMap.putIfAbsent(entry1.getKey(),
					LoadedConfigProperty.withCopyFrom(entry1.getValue()).source(source).build()));
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
		settings.getListeners().entrySet().forEach(entry -> {
			try {
				entry.getValue().onEvent(event);
			} catch (Exception e) {
				LOGGER.error("Exception occurred on configEventListener: {}, event: {}, cause: {}", entry.getKey(), event, e, e);
			}
		});
	}

	private void invokeCallbacks(ConfigEvent event) {
		Collection<BiConsumer> collection = propertyCallbacks.get(event.getName());
		if (collection != null && !collection.isEmpty()) {
			for (BiConsumer callback : collection) {
				try {
					//noinspection unchecked
					callback.accept(event.getOldValue(), event.getNewValue());
				} catch (Exception e) {
					LOGGER.error("Exception occurred on property-change callback: {}, event: {}, cause: {}", callback, event, e, e);
				}
			}
		}
	}

	//// ConfigProperty classes

	private class ConfigPropertyImpl implements ConfigProperty {
		private final String name;
		private final Function<String, ?> valueParser;

		ConfigPropertyImpl(String name) {
			this(name, str -> str);
		}

		ConfigPropertyImpl(String name, Function<String, ?> valueParser) {
			this.name = name;
			this.valueParser = valueParser;
		}

		@Override
		public final Optional<String> getSource() {
			return Optional.ofNullable(propertyMap.get(name)).flatMap(ConfigProperty::getSource);
		}

		@Override
		public Optional<String> getOrigin() {
			return Optional.ofNullable(propertyMap.get(name)).flatMap(ConfigProperty::getOrigin);
		}

		@Override
		public final String getName() {
			return name;
		}

		@Override
		public final Optional<String> getAsString() {
			return Optional.ofNullable(propertyMap.get(name)).flatMap(ConfigProperty::getAsString);
		}

		@Override
		public final String getAsString(String defaultValue) {
			return getAsString().orElse(defaultValue);
		}

		private void addCallbackInternal(BiConsumer callback) {
			BiConsumer<String, String> callback1 = (oldValue, newValue) -> {
				Object oldValue1 = oldValue != null ? valueParser.apply(oldValue) : null;
				Object newValue1 = newValue != null ? valueParser.apply(newValue) : null;
				//noinspection unchecked
				callback.accept(oldValue1, newValue1);
			};
			propertyCallbacks.computeIfAbsent(name, name -> new CopyOnWriteArrayList<>());
			propertyCallbacks.get(name).add(callback1);
		}
	}

	private class DoubleConfigPropertyImpl extends ConfigPropertyImpl implements DoubleConfigProperty {

		DoubleConfigPropertyImpl(String name) {
			super(name, Double::parseDouble);
		}

		@Override
		public Optional<Double> get() {
			return super.getAsString().map(Double::parseDouble);
		}

		@Override
		public double get(double defaultValue) {
			return get().orElse(defaultValue);
		}

		@Override
		public void addCallback(BiConsumer<Double, Double> callback) {
			super.addCallbackInternal(callback);
		}
	}

	private class LongConfigPropertyImpl extends ConfigPropertyImpl implements LongConfigProperty {

		LongConfigPropertyImpl(String name) {
			super(name, Long::parseLong);
		}

		@Override
		public Optional<Long> get() {
			return super.getAsString().map(Long::parseLong);
		}

		@Override
		public long get(long defaultValue) {
			return get().orElse(defaultValue);
		}

		@Override
		public void addCallback(BiConsumer<Long, Long> callback) {
			super.addCallbackInternal(callback);
		}
	}

	private class BooleanConfigPropertyImpl extends ConfigPropertyImpl implements BooleanConfigProperty {

		BooleanConfigPropertyImpl(String name) {
			super(name, Boolean::parseBoolean);
		}

		@Override
		public Optional<Boolean> get() {
			return super.getAsString().map(Boolean::parseBoolean);
		}

		@Override
		public boolean get(boolean defaultValue) {
			return get().orElse(defaultValue);
		}

		@Override
		public void addCallback(BiConsumer<Boolean, Boolean> callback) {
			super.addCallbackInternal(callback);
		}
	}

	private class IntConfigPropertyImpl extends ConfigPropertyImpl implements IntConfigProperty {

		IntConfigPropertyImpl(String name) {
			super(name, Integer::parseInt);
		}

		@Override
		public Optional<Integer> get() {
			return super.getAsString().map(Integer::parseInt);
		}

		@Override
		public int get(int defaultValue) {
			return get().orElse(defaultValue);
		}

		@Override
		public void addCallback(BiConsumer<Integer, Integer> callback) {
			super.addCallbackInternal(callback);
		}
	}

	private class StringConfigPropertyImpl extends ConfigPropertyImpl implements StringConfigProperty {

		StringConfigPropertyImpl(String name) {
			super(name);
		}

		@Override
		public Optional<String> get() {
			return super.getAsString();
		}

		@Override
		public String get(String defaultValue) {
			return super.getAsString(defaultValue);
		}

		@Override
		public void addCallback(BiConsumer<String, String> callback) {
			super.addCallbackInternal(callback);
		}
	}

}
