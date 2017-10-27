package io.scalecube.config.source;

import io.scalecube.config.ConfigProperty;

import java.util.Map;
import java.util.Optional;

// Helper class
public final class LoadedConfigProperty implements ConfigProperty {
  private final String name; // not null
  private final String source; // nullable
  private final String origin; // nullable
  private final String value; // nullable

  private LoadedConfigProperty(Builder builder) {
    this.name = builder.name;
    this.source = builder.source;
    this.origin = builder.origin;
    this.value = builder.value;
  }

  public static LoadedConfigProperty forNameAndValue(String name, String value) {
    return withNameAndValue(name, value).build();
  }

  public static Builder withNameAndValue(Map.Entry<String, String> entry) {
    return withNameAndValue(entry.getKey(), entry.getValue());
  }

  public static Builder withNameAndValue(String name, String value) {
    return new Builder(name, value);
  }

  public static Builder withCopyFrom(ConfigProperty property) {
    Builder builder = new Builder(property.name(), property.valueAsString(null));
    builder.source = property.source().orElse(null);
    builder.origin = property.origin().orElse(null);
    return builder;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Optional<String> source() {
    return Optional.ofNullable(source);
  }

  @Override
  public Optional<String> origin() {
    return Optional.ofNullable(origin);
  }

  @Override
  public Optional<String> valueAsString() {
    return Optional.ofNullable(value);
  }

  @Override
  public String valueAsString(String defaultValue) {
    return valueAsString().orElse(defaultValue);
  }

  @Override
  public String toString() {
    return "LoadedConfigProperty{" +
        "name=" + name +
        ", source='" + source + '\'' +
        ", origin='" + origin + '\'' +
        ", value='" + value + '\'' +
        '}';
  }

  public static class Builder {
    private final String name;
    private final String value;
    private String source;
    private String origin;

    public Builder(String name, String value) {
      this.name = name;
      this.value = value;
    }

    public Builder source(String source) {
      this.source = source;
      return this;
    }

    public Builder origin(String origin) {
      this.origin = origin;
      return this;
    }

    public LoadedConfigProperty build() {
      return new LoadedConfigProperty(this);
    }
  }
}
