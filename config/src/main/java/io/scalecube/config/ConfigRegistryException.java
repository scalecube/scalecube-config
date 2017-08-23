package io.scalecube.config;

public abstract class ConfigRegistryException extends RuntimeException {

  public ConfigRegistryException() {}

  public ConfigRegistryException(String message) {
    super(message);
  }

  public ConfigRegistryException(String message, Throwable cause) {
    super(message, cause);
  }

  public ConfigRegistryException(Throwable cause) {
    super(cause);
  }
}
