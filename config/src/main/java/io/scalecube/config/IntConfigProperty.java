package io.scalecube.config;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

public interface IntConfigProperty extends ConfigProperty {

  Optional<Integer> value();

  int value(int defaultValue);

  /**
   * @throws NoSuchElementException if value is null
   */
  int valueOrThrow();

  void addCallback(BiConsumer<Integer, Integer> callback);

  void addCallback(Executor executor, BiConsumer<Integer, Integer> callback);
}
