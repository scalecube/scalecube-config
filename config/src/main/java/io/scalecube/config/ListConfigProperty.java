package io.scalecube.config;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

public interface ListConfigProperty<T> extends ConfigProperty {

  Optional<List<T>> value();

  List<T> value(List<T> defaultValue);

  void addCallback(BiConsumer<List<T>, List<T>> callback);

  void addCallback(Executor executor, BiConsumer<List<T>, List<T>> callback);
}
