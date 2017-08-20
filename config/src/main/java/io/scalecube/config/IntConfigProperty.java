package io.scalecube.config;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

public interface IntConfigProperty extends ConfigProperty {

  Optional<Integer> get();

  int get(int defaultValue);

  void setCallback(BiConsumer<Integer, Integer> callback);

  void setCallback(ExecutorService executor, BiConsumer<Integer, Integer> callback);
}
