package io.scalecube.config.source;

import io.scalecube.config.ConfigProperty;

import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class SystemPropertiesConfigSource implements ConfigSource {

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    Properties properties = System.getProperties();
    Map<String, ConfigProperty> result = new TreeMap<>();
    for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements();) {
      String propName = (String) e.nextElement();
      result.put(propName, LoadedConfigProperty.forNameAndValue(propName, properties.getProperty(propName)));
    }
    return result;
  }
}
