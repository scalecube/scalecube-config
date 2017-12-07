package io.scalecube.config.keyvalue;

import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Generic entity class for key-value config storage.
 */
public final class KeyValueConfigEntity {
  /**
   * A group name. Non persistent nullable field. See {@link #createWithGroup(String)}.
   */
  private String groupName;

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
   * Enhances this object entity with non-persistent groupName.
   *
   * @param groupName group name; may be null.
   * @return new object with groupName.
   */
  public KeyValueConfigEntity createWithGroup(@Nullable String groupName) {
    KeyValueConfigEntity entity = new KeyValueConfigEntity();
    entity.propName = this.propName;
    entity.propValue = this.propValue;
    entity.disabled = this.disabled;
    entity.groupName = groupName;
    return entity;
  }

  public Optional<String> getGroupName() {
    return Optional.ofNullable(groupName);
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
        "groupName='" + groupName + '\'' +
        ", propName='" + propName + '\'' +
        ", propValue='" + propValue + '\'' +
        ", disabled=" + disabled +
        '}';
  }
}
