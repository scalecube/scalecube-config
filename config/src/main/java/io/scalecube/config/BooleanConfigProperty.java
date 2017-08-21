package io.scalecube.config;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

public interface BooleanConfigProperty extends ConfigProperty {

  Optional<Boolean> value();

  boolean value(boolean defaultValue);

  void addCallback(BiConsumer<Boolean, Boolean> callback);

  void addCallback(Executor executor, BiConsumer<Boolean, Boolean> callback);
}
