package io.scalecube.config.source;

import io.scalecube.config.ConfigProperty;
import java.util.Map;
import java.util.Optional;

// Helper class
public final class LoadedObjectConfigProperty<T> implements ConfigProperty {
  private final String name; // not null
  private final String source; // nullable
  private final String origin; // nullable
  private final T value; // nullable

  private LoadedObjectConfigProperty(Builder<T> builder) {
    this.name = builder.name;
    this.source = builder.source;
    this.origin = builder.origin;
    this.value = builder.value;
  }

  public static <T> LoadedObjectConfigProperty<T> forNameAndValue(String name, T value) {
    return withNameAndValue(name, value).build();
  }

  public static <T> Builder<T> withNameAndValue(Map.Entry<String, T> entry) {
    return withNameAndValue(entry.getKey(), entry.getValue());
  }

  public static <T> Builder<T> withNameAndValue(String name, T value) {
    return new Builder<>(name, value);
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
    return Optional.ofNullable(value).map(Object::toString);
  }

  @Override
  public String valueAsString(String defaultValue) {
    return valueAsString().orElse(defaultValue);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("LoadedConfigProperty{");
    sb.append("name='").append(name).append('\'');
    sb.append(", source='").append(source).append('\'');
    sb.append(", origin='").append(origin).append('\'');
    sb.append(", value='").append(value).append('\'');
    sb.append('}');
    return sb.toString();
  }

  public static class Builder<T> {
    private final String name;
    private final T value;
    private String source;
    private String origin;

    public Builder(String name, T value) {
      this.name = name;
      this.value = value;
    }

    public Builder<T> source(String source) {
      this.source = source;
      return this;
    }

    public Builder<T> origin(String origin) {
      this.origin = origin;
      return this;
    }

    public LoadedObjectConfigProperty<T> build() {
      return new LoadedObjectConfigProperty<>(this);
    }
  }
}
