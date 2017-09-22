package io.scalecube.config;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public interface LongConfigProperty extends ConfigProperty {

  Optional<Long> value();

  long value(long defaultValue);

  /**
   * @throws NoSuchElementException if value is null
   */
  long valueOrThrow();

  void addCallback(BiConsumer<Long, Long> callback);

  void addCallback(Executor executor, BiConsumer<Long, Long> callback);

  void addValidator(Predicate<Long> validator);
}
