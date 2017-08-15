package io.scalecube.config.audit;

import io.scalecube.config.ConfigProperty;

import java.util.Date;
import java.util.Objects;

import javax.annotation.Nonnull;

public final class ConfigEvent {

  public enum Type {
    ADDED, REMOVED, UPDATED
  }

  private final String name;
  private final Date timestamp;
  private final Type type;
  private final String host;

  private final String oldValue;
  private final String oldSource;
  private final String oldOrigin;

  private final String newValue;
  private final String newSource;
  private final String newOrigin;

  private ConfigEvent(String name, Type type, String host, ConfigProperty oldProp, ConfigProperty newProp) {
    this.name = Objects.requireNonNull(name, "ConfigEvent: propName is required");
    this.timestamp = new Date();
    this.type = type;
    this.host = host;

    this.oldValue = oldProp != null ? oldProp.getAsString().orElse(null) : null;
    this.oldSource = oldProp != null ? oldProp.getSource().orElse(null) : null;
    this.oldOrigin = oldProp != null ? oldProp.getOrigin().orElse(null) : null;

    this.newValue = newProp != null ? newProp.getAsString().orElse(null) : null;
    this.newSource = newProp != null ? newProp.getSource().orElse(null) : null;
    this.newOrigin = newProp != null ? newProp.getOrigin().orElse(null) : null;
  }

  public static ConfigEvent createAdded(@Nonnull String propName, String host, @Nonnull ConfigProperty newProp) {
    Objects.requireNonNull(newProp, "ConfigEvent: newProp is required");
    return new ConfigEvent(propName, Type.ADDED, host, null, newProp);
  }

  public static ConfigEvent createRemoved(@Nonnull String propName, String host, @Nonnull ConfigProperty oldProp) {
    Objects.requireNonNull(oldProp, "ConfigEvent: oldProp is required");
    return new ConfigEvent(propName, Type.REMOVED, host, oldProp, null);
  }

  public static ConfigEvent createUpdated(@Nonnull String propName, String host, ConfigProperty oldProp,
      ConfigProperty newProp) {
    Objects.requireNonNull(newProp, "ConfigEvent: newProp is required");
    Objects.requireNonNull(oldProp, "ConfigEvent: oldProp is required");
    return new ConfigEvent(propName, Type.UPDATED, host, oldProp, newProp);
  }

  public @Nonnull String getName() {
    return name;
  }

  public @Nonnull Date getTimestamp() {
    return timestamp;
  }

  public @Nonnull Type getType() {
    return type;
  }

  public String getHost() {
    return host;
  }

  public String getOldValue() {
    return oldValue;
  }

  public String getOldSource() {
    return oldSource;
  }

  public String getOldOrigin() {
    return oldOrigin;
  }

  public String getNewValue() {
    return newValue;
  }

  public String getNewSource() {
    return newSource;
  }

  public String getNewOrigin() {
    return newOrigin;
  }

  @Override
  public String toString() {
    return "ConfigEvent{" +
        "name='" + name + '\'' +
        ", timestamp=" + timestamp +
        ", type=" + type +
        ", host='" + host + '\'' +
        ", oldValue='" + oldValue + '\'' +
        ", oldSource='" + oldSource + '\'' +
        ", oldOrigin='" + oldOrigin + '\'' +
        ", newValue='" + newValue + '\'' +
        ", newSource='" + newSource + '\'' +
        ", newOrigin='" + newOrigin + '\'' +
        '}';
  }
}
