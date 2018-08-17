package io.scalecube.config;

public class ConfigPropertyInfo {
  private String name;
  private String value;
  private String source;
  private String origin;
  private String host;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ConfigPropertyInfo{");
    sb.append("name='").append(name).append('\'');
    sb.append(", value='").append(value).append('\'');
    sb.append(", source='").append(source).append('\'');
    sb.append(", origin='").append(origin).append('\'');
    sb.append(", host='").append(host).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
