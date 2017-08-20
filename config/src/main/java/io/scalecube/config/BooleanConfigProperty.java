package io.scalecube.config;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

public interface BooleanConfigProperty extends ConfigProperty {

  Optional<Boolean> get();

  boolean get(boolean defaultValue);

  void setCallback(BiConsumer<Boolean, Boolean> callback);

  void setCallback(ExecutorService executor, BiConsumer<Boolean, Boolean> callback);
}
