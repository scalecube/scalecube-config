package io.scalecube.config;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

public interface DoubleConfigProperty extends ConfigProperty {

  Optional<Double> get();

  double get(double defaultValue);

  void setCallback(BiConsumer<Double, Double> callback);

  void setCallback(ExecutorService executor, BiConsumer<Double, Double> callback);
}
