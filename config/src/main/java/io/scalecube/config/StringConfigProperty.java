package io.scalecube.config;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

public interface StringConfigProperty extends ConfigProperty {

  Optional<String> get();

  String get(String defaultValue);

  void setCallback(BiConsumer<String, String> callback);

  void setCallback(ExecutorService executor, BiConsumer<String, String> callback);
}
