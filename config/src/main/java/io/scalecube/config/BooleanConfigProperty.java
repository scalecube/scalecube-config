package io.scalecube.config;

import java.util.Optional;
import java.util.function.BiConsumer;

public interface BooleanConfigProperty extends ConfigProperty {

  Optional<Boolean> get();

  boolean get(boolean defaultValue);

  void addCallback(BiConsumer<Boolean, Boolean> callback);
}
