package io.scalecube.config;

import java.util.Optional;

public interface ConfigProperty {

  String name();

  Optional<String> source();

  Optional<String> origin();

  Optional<String> valueAsString();

  String valueAsString(String defaultValue);
}
