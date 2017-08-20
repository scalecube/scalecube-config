package io.scalecube.config;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

public interface LongConfigProperty extends ConfigProperty {

  Optional<Long> value();

  long value(long defaultValue);

  void addCallback(BiConsumer<Long, Long> callback);

  void addCallback(Executor executor, BiConsumer<Long, Long> callback);
}
