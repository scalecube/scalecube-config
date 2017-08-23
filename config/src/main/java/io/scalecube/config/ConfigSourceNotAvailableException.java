package io.scalecube.config;

public class ConfigSourceNotAvailableException extends ConfigRegistryException {

  public ConfigSourceNotAvailableException() {}

  public ConfigSourceNotAvailableException(String message) {
    super(message);
  }

  public ConfigSourceNotAvailableException(String message, Throwable cause) {
    super(message, cause);
  }

  public ConfigSourceNotAvailableException(Throwable cause) {
    super(cause);
  }
}
