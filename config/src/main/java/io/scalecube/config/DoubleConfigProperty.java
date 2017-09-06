package io.scalecube.config;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

public interface DoubleConfigProperty extends ConfigProperty {

  Optional<Double> value();

  double value(double defaultValue);

  /**
   * @throws NoSuchElementException if value is null
   */
  double valueOrThrow();

  void addCallback(BiConsumer<Double, Double> callback);

  void addCallback(Executor executor, BiConsumer<Double, Double> callback);
}
