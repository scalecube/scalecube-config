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
    return "ConfigSourceInfo{sourceName=" + sourceName +
        ", priorityOrder=" + priorityOrder +
        ", configSourceString='" + configSourceString + '\'' +
        ", healthString='" + healthString + '\'' +
        ", host=" + host +
        '}';
  }
}
