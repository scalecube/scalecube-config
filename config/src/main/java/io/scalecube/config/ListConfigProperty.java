package io.scalecube.config;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

public interface ListConfigProperty<T> extends ConfigProperty {

  Optional<List<T>> value();

  List<T> value(List<T> defaultValue);

  /**
   * @throws NoSuchElementException if value is null
   */
  List<T> valueOrThrow();

  void addCallback(BiConsumer<List<T>, List<T>> callback);

  void addCallback(Executor executor, BiConsumer<List<T>, List<T>> callback);
}
