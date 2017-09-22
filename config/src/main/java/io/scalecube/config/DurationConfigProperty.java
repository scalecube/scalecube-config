package io.scalecube.config;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public interface DurationConfigProperty extends ConfigProperty {

  Optional<Duration> value();

  Duration value(Duration defaultValue);

  /**
   * @throws NoSuchElementException if value is null
   */
  Duration valueOrThrow();

  void addCallback(BiConsumer<Duration, Duration> callback);

  void addCallback(Executor executor, BiConsumer<Duration, Duration> callback);

  void addValidator(Predicate<Duration> validator);
}
