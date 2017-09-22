package io.scalecube.config;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public interface BooleanConfigProperty extends ConfigProperty {

  Optional<Boolean> value();

  boolean value(boolean defaultValue);

  /**
   * @throws NoSuchElementException if value is null
   */
  boolean valueOrThrow();

  void addCallback(BiConsumer<Boolean, Boolean> callback);

  void addCallback(Executor executor, BiConsumer<Boolean, Boolean> callback);

  void addValidator(Predicate<Boolean> validator);
}
