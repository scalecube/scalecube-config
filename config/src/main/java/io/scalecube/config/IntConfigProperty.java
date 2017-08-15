package io.scalecube.config;

import java.util.Optional;
import java.util.function.BiConsumer;

public interface IntConfigProperty extends ConfigProperty {

  Optional<Integer> get();

  int get(int defaultValue);

  void addCallback(BiConsumer<Integer, Integer> callback);
}
