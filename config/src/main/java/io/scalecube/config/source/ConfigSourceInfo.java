package io.scalecube.config.source;

public class ConfigSourceInfo {
  private String sourceName;
  private int priorityOrder;
  private String configSourceString;
  private String healthString;
  private String host;

  public String getSourceName() {
    return sourceName;
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  public int getPriorityOrder() {
    return priorityOrder;
  }

  public void setPriorityOrder(int priorityOrder) {
    this.priorityOrder = priorityOrder;
  }

  public String getConfigSourceString() {
    return configSourceString;
  }

  public void setConfigSourceString(String configSourceString) {
    this.configSourceString = configSourceString;
  }

  public String getHealthString() {
    return healthString;
  }

  public void setHealthString(String healthString) {
    this.healthString = healthString;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ConfigSourceInfo{");
    sb.append("sourceName='").append(sourceName).append('\'');
    sb.append(", priorityOrder=").append(priorityOrder);
    sb.append(", configSourceString='").append(configSourceString).append('\'');
    sb.append(", healthString='").append(healthString).append('\'');
    sb.append(", host='").append(host).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
