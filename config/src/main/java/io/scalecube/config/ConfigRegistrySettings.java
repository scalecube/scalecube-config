package io.scalecube.config;

import io.scalecube.config.audit.ConfigEventListener;
import io.scalecube.config.source.ConfigSource;

import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Anton Kharenko
 */
public final class ConfigRegistrySettings {

	public static final int DEFAULT_RELOAD_PERIOD_SEC = 15;
	public static final int DEFAULT_RECENT_EVENTS_NUM = 30;
	public static final boolean DEFAULT_JMX_ENABLED = true;
	public static final String DEFAULT_JMX_MBEAN_NAME = "io.scalecube.config:name=ConfigRegistry";

	private final int reloadIntervalSec;
	private final int recentConfigEventsNum;
	private final Map<String, ConfigEventListener> listeners;
	private final Map<String, ConfigSource> sources;
	private final String host;
	private final boolean jmxEnabled;
	private final String jmxMBeanName;

	private ConfigRegistrySettings(Builder builder) {
		this.reloadIntervalSec = builder.reloadIntervalSec;
		this.recentConfigEventsNum = builder.recentConfigEventsNum;
		this.listeners = Collections.unmodifiableMap(builder.listeners);
		this.sources = Collections.unmodifiableMap(builder.sources);
		this.host = builder.host != null ? builder.host : resolveHost();
		this.jmxEnabled = builder.jmxEnabled;
		this.jmxMBeanName = builder.jmxMBeanName;
	}

	private static String resolveHost() {
		try {
			return InetAddress.getLocalHost().getCanonicalHostName();
		} catch (Exception e) {
			return "unresolved";
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public int getReloadIntervalSec() {
		return reloadIntervalSec;
	}

	public int getRecentConfigEventsNum() {
		return recentConfigEventsNum;
	}

	public Map<String, ConfigEventListener> getListeners() {
		return listeners;
	}

	public Map<String, ConfigSource> getSources() {
		return sources;
	}

	public String getHost() {
		return host;
	}

	public boolean isJmxEnabled() {
		return jmxEnabled;
	}

	public String getJmxMBeanName() {
		return jmxMBeanName;
	}

	@Override
	public String toString() {
		return "ConfigRegistrySettings{" +
				"reloadIntervalSec=" + reloadIntervalSec +
				", recentConfigEventsNum=" + recentConfigEventsNum +
				", listeners=" + listeners +
				", sources=" + sources +
				", host='" + host + '\'' +
				", jmxEnabled=" + jmxEnabled +
				", jmxMBeanName='" + jmxMBeanName + '\'' +
				'}';
	}

	public static class Builder {
		private int reloadIntervalSec = DEFAULT_RELOAD_PERIOD_SEC;
		private int recentConfigEventsNum = DEFAULT_RECENT_EVENTS_NUM;
		private Map<String, ConfigEventListener> listeners = new LinkedHashMap<>();
		private Map<String, ConfigSource> sources = new LinkedHashMap<>();
		private String host = null;
		private boolean jmxEnabled = DEFAULT_JMX_ENABLED;
		private String jmxMBeanName = DEFAULT_JMX_MBEAN_NAME;

		private Builder() {
		}

		public Builder reloadIntervalSec(int reloadPeriodSec) {
			this.reloadIntervalSec = reloadPeriodSec;
			return this;
		}

		public Builder keepRecentConfigEvents(int recentConfigEventsNum) {
			this.recentConfigEventsNum = recentConfigEventsNum;
			return this;
		}

		public Builder addListener(ConfigEventListener configEventListener) {
			this.listeners.put(configEventListener.getClass().getSimpleName(), configEventListener);
			return this;
		}

		public Builder addLastSource(String name, ConfigSource configSource) {
			sources.put(name, configSource);
			return this;
		}

		public Builder jmxEnabled(boolean jmxEnabled) {
			this.jmxEnabled = jmxEnabled;
			return this;
		}

		public Builder jmxMBeanName(String jmxMBeanName) {
			this.jmxMBeanName = jmxMBeanName;
			return this;
		}

		public ConfigRegistrySettings build() {
			return new ConfigRegistrySettings(this);
		}
	}

}
