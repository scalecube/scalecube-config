package io.scalecube.config;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

class MappedObjectConfigProperty<T> implements ObjectConfigProperty<T> {

  private final StringConfigProperty configProperty;
  private final Function<String, T> mapper;

  MappedObjectConfigProperty(StringConfigProperty configProperty, Function<String, T> mapper) {
    this.configProperty = configProperty;
    this.mapper = mapper;
  }

  @Override
  public String name() {
    return configProperty.name();
  }

  @Override
  public Optional<T> value() {
    return configProperty.value().map(mapper);
  }

  @Override
  public T value(T defaultValue) {
    return value().orElse(defaultValue);
  }

  @Override
  public void addCallback(BiConsumer<T, T> callback) {
    configProperty.addCallback(
        (v0, v1) -> {
          T t0 = Optional.ofNullable(v0).map(mapper).orElse(null);
          T t1 = Optional.ofNullable(v1).map(mapper).orElse(null);
          callback.accept(t0, t1);
        });
  }

  @Override
  public void addCallback(Executor executor, BiConsumer<T, T> callback) {
    configProperty.addCallback(
        executor,
        (v0, v1) -> {
          T t0 = Optional.ofNullable(v0).map(mapper).orElse(null);
          T t1 = Optional.ofNullable(v1).map(mapper).orElse(null);
          callback.accept(t0, t1);
        });
  }

  @Override
  public void addValidator(Predicate<T> validator) {
    configProperty.addValidator(
        value -> validator.test(Optional.ofNullable(value).map(mapper).orElse(null)));
  }
}
