package io.scalecube.config.source;

import io.scalecube.config.ConfigProperty;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

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

  public static LoadedConfigProperty forNameAndValue(@Nonnull String name, @Nonnull String value) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);
    return new Builder(name, value).build();
  }

  public static Builder withNameAndValue(@Nonnull Map.Entry<String, String> entry) {
    Objects.requireNonNull(entry);
    return withNameAndValue(entry.getKey(), entry.getValue());
  }

  public static Builder withNameAndValue(@Nonnull String name, @Nonnull String value) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);
    return new Builder(name, value);
  }

  public static Builder withCopyFrom(@Nonnull ConfigProperty property) {
    Builder builder = new Builder(property.getName(), Objects.requireNonNull(property.getAsString(null)));
    builder.source = property.getSource().orElse(null);
    builder.origin = property.getOrigin().orElse(null);
    return builder;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Optional<String> getSource() {
    return Optional.ofNullable(source);
  }

  @Override
  public Optional<String> getOrigin() {
    return Optional.ofNullable(origin);
  }

  @Override
  public Optional<String> getAsString() {
    return Optional.ofNullable(value);
  }

  @Override
  public String getAsString(String defaultValue) {
    return getAsString().orElse(defaultValue);
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

    public Builder(String name) {
      this(name, null);
    }

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
