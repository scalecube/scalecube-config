package io.scalecube.config.keyvalue;

import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * Generic entity class for key-value config data source.
 */
public final class KeyValueConfigEntity {
  /**
   * A config name. Non-persistent field. Being set in method {@link #setConfigName(KeyValueConfigName)}.
   */
  private KeyValueConfigName configName;

  /**
   * Property name. Persistent not-nullable field.
   */
  private String propName;

  /**
   * Property value. Persistent not-nullable field.
   */
  private String propValue;

  /**
   * Persistent indicator flag denoting intent to have actually the property key-value pair in data source but have it
   * in disabled state.
   */
  private boolean disabled;

  public KeyValueConfigEntity() {}

  /**
   * <b>NOTE:</b> this constructor exposed for test purpose only.
   */
  KeyValueConfigEntity(String propName, String propValue, KeyValueConfigName configName) {
    this.configName = configName;
    this.propName = propName;
    this.propValue = propValue;
  }

  /**
   * Enhances this entity object with non-persistent configName, returns a copy.
   *
   * @param configName config name from where this entity object was loaded.
   * @return copy of this object with configName.
   */
  public KeyValueConfigEntity setConfigName(@Nonnull KeyValueConfigName configName) {
    Objects.requireNonNull(configName);
    KeyValueConfigEntity entity = new KeyValueConfigEntity();
    entity.configName = configName;
    entity.propName = this.propName;
    entity.propValue = this.propValue;
    entity.disabled = this.disabled;
    return entity;
  }

  public KeyValueConfigName getConfigName() {
    return configName;
  }

  public String getPropName() {
    return propName;
  }

  public void setPropName(String propName) {
    this.propName = propName;
  }

  public String getPropValue() {
    return propValue;
  }

  public void setPropValue(String propValue) {
    this.propValue = propValue;
  }

  public boolean getDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }

  @Override
  public String toString() {
    return "KeyValueConfigEntity{" +
        "configName=" + configName +
        ", propName='" + propName + '\'' +
        ", propValue='" + propValue + '\'' +
        ", disabled=" + disabled +
        '}';
  }
}
