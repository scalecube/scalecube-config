package io.scalecube.config.keyvalue;

public final class KeyValueConfigEntity {
  private String groupName; // nullable
  private String propName; // not null
  private String propValue; // not null
  private boolean disabled; // nullable

  public KeyValueConfigEntity() {}

  public KeyValueConfigEntity(String groupName, KeyValueConfigEntity other) {
    this.groupName = groupName;
    this.propName = other.propName;
    this.propValue = other.propValue;
    this.disabled = other.disabled;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
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
