package io.scalecube.config;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

public interface ObjectConfigProperty<T> {

  String name();

  Optional<T> value();

  T value(T defaultValue);

  void addCallback(BiConsumer<T, T> callback);

  void addCallback(Executor executor, BiConsumer<T, T> callback);
}
