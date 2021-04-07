package io.scalecube.config.source;

import io.scalecube.config.ConfigProperty;
import java.util.Map;
import java.util.TreeMap;

public final class SystemEnvironmentSingleVariableConfigSource implements ConfigSource {

  private static final String VAR_NAME = "SETTINGS";

  private static final String DELIMITER = ";";
  private static final String SEPARATOR = "=";

  private final String varName;
  private final String delimiter;
  private final String separator;

  private Map<String, ConfigProperty> loadedConfig;

  public SystemEnvironmentSingleVariableConfigSource() {
    this(VAR_NAME, DELIMITER, SEPARATOR);
  }

  /**
   * Constructor.
   *
   * @param varName varName
   * @param delimiter delimiter
   * @param separator separator
   */
  public SystemEnvironmentSingleVariableConfigSource(
      String varName, String delimiter, String separator) {
    this.varName = varName;
    this.delimiter = delimiter;
    this.separator = separator;
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    if (loadedConfig != null) {
      return loadedConfig;
    }

    Map<String, ConfigProperty> result = new TreeMap<>();

    String settings = System.getenv(varName);

    if (settings != null && !settings.isEmpty()) {
      for (String str : settings.split(delimiter)) {
        String[] split = str.split(separator);
        if (split.length != 2) {
          break;
        }

        String propName = split[0];
        String propValue = split[1];

        // store value as system property under mapped name
        System.setProperty(propName, propValue);
        result.put(propName, LoadedConfigProperty.forNameAndValue(propName, propValue));
      }
    }

    return loadedConfig = result;
  }
}
