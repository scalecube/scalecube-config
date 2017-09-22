package io.scalecube.config;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public interface StringConfigProperty extends ConfigProperty {

  Optional<String> value();

  String value(String defaultValue);

  /**
   * @throws NoSuchElementException if value is null
   */
  String valueOrThrow();

  void addCallback(BiConsumer<String, String> callback);

  void addCallback(Executor executor, BiConsumer<String, String> callback);

  void addValidator(Predicate<String> validator);
}
