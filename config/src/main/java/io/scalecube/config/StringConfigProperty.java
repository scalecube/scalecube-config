package io.scalecube.config;

import java.util.Optional;
import java.util.function.BiConsumer;

public interface StringConfigProperty extends ConfigProperty {

  Optional<String> get();

  String get(String defaultValue);

  void addCallback(BiConsumer<String, String> callback);
}
