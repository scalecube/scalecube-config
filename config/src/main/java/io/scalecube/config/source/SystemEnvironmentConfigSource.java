package io.scalecube.config.source;

import io.scalecube.config.ConfigProperty;

import java.util.Map;
import java.util.TreeMap;

public class SystemEnvironmentConfigSource implements ConfigSource {

	@Override
	public Map<String, ConfigProperty> loadConfig() {
		Map<String, String> env = System.getenv();
		Map<String, ConfigProperty> result = new TreeMap<>();
		for (Map.Entry<String, String> entry : env.entrySet()) {
			String propName = entry.getKey();
			result.put(propName, LoadedConfigProperty.forNameAndValue(propName, entry.getValue()));
		}
		return result;
	}
}
