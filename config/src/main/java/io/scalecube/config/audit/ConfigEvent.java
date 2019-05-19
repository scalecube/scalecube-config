package io.scalecube.config.audit;

import io.scalecube.config.ConfigProperty;
import java.util.Date;
import java.util.Objects;

public final class ConfigEvent {

  public enum Type {
    ADDED,
    REMOVED,
    UPDATED
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

  private ConfigEvent(
      String name, Type type, String host, ConfigProperty oldProp, ConfigProperty newProp) {
    this.name = Objects.requireNonNull(name, "ConfigEvent: propName is required");
    this.timestamp = new Date();
    this.type = type;
    this.host = host;

    this.oldValue = oldProp != null ? oldProp.valueAsString().orElse(null) : null;
    this.oldSource = oldProp != null ? oldProp.source().orElse(null) : null;
    this.oldOrigin = oldProp != null ? oldProp.origin().orElse(null) : null;

    this.newValue = newProp != null ? newProp.valueAsString().orElse(null) : null;
    this.newSource = newProp != null ? newProp.source().orElse(null) : null;
    this.newOrigin = newProp != null ? newProp.origin().orElse(null) : null;
  }

  /**
   * Creates {@link Type#ADDED} event for particular property.
   *
   * @param propName property name
   * @param host host
   * @param newProp added property
   * @return config event
   */
  public static ConfigEvent createAdded(String propName, String host, ConfigProperty newProp) {
    Objects.requireNonNull(newProp, "ConfigEvent: newProp is required");
    return new ConfigEvent(propName, Type.ADDED, host, null, newProp);
  }

  /**
   * Creates {@link Type#REMOVED} event for particular property.
   *
   * @param propName property name
   * @param host host
   * @param oldProp removed property
   * @return config event
   */
  public static ConfigEvent createRemoved(String propName, String host, ConfigProperty oldProp) {
    Objects.requireNonNull(oldProp, "ConfigEvent: oldProp is required");
    return new ConfigEvent(propName, Type.REMOVED, host, oldProp, null);
  }

  /**
   * Creates {@link Type#UPDATED} event for particular property.
   *
   * @param propName property name
   * @param host host
   * @param oldProp old property
   * @param newProp new property
   * @return config event
   */
  public static ConfigEvent createUpdated(
      String propName, String host, ConfigProperty oldProp, ConfigProperty newProp) {
    Objects.requireNonNull(newProp, "ConfigEvent: newProp is required");
    Objects.requireNonNull(oldProp, "ConfigEvent: oldProp is required");
    return new ConfigEvent(propName, Type.UPDATED, host, oldProp, newProp);
  }

  public String getName() {
    return name;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public Type getType() {
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

  /**
   * Checks if property is changed.
   *
   * @return true if property is changed
   */
  public boolean isChanged() {
    if (type == Type.ADDED || type == Type.REMOVED) {
      return true;
    }
    return !Objects.equals(this.oldSource, this.newSource)
        || !Objects.equals(this.oldOrigin, this.newOrigin)
        || !Objects.equals(this.oldValue, this.newValue);
  }

  @Override
  public String toString() {
    return "{\"name\":\" "
        + name
        + "\",\"timestamp\":\""
        + timestamp
        + "\",\"type\":\""
        + type
        + "\",\"host\":\""
        + host
        + "\",\"oldValue\":\""
        + oldValue
        + "\",\"oldSource\":\""
        + oldSource
        + "\",\"oldOrigin\":\""
        + oldOrigin
        + "\",\"newValue\":\""
        + newValue
        + "\",\"newSource\":\""
        + newSource
        + "\",\"newOrigin\":\""
        + newOrigin
        + "\"}";
  }
}
