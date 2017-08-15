package io.scalecube.config.mongo;

public class ConfigurationEntity {
  private String propName; // not null
  private String groupName; // nullable
  private String propValue; // nullable
  private Boolean enabled; // nullable

  public String getPropName() {
    return propName;
  }

  public void setPropName(String propName) {
    this.propName = propName;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public String getPropValue() {
    return propValue;
  }

  public void setPropValue(String propValue) {
    this.propValue = propValue;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ConfigurationEntity that = (ConfigurationEntity) o;

    if (propName != null ? !propName.equals(that.propName) : that.propName != null)
      return false;
    return groupName != null ? groupName.equals(that.groupName) : that.groupName == null;

  }

  @Override
  public int hashCode() {
    int result = propName != null ? propName.hashCode() : 0;
    result = 31 * result + (groupName != null ? groupName.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ConfigurationEntity{" +
        "propName=" + propName +
        ", groupName='" + groupName + '\'' +
        ", propValue='" + propValue + '\'' +
        ", enabled=" + enabled +
        '}';
  }
}
