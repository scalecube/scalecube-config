package io.scalecube.config;

import java.util.Optional;
import java.util.function.BiConsumer;

public interface LongConfigProperty extends ConfigProperty {

  Optional<Long> get();

  long get(long defaultValue);

  void addCallback(BiConsumer<Long, Long> callback);
}
