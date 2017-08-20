package io.scalecube.config;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

public interface LongConfigProperty extends ConfigProperty {

  Optional<Long> get();

  long get(long defaultValue);

  void setCallback(BiConsumer<Long, Long> callback);

  void setCallback(ExecutorService executor, BiConsumer<Long, Long> callback);
}
