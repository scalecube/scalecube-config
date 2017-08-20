package io.scalecube.config;

import java.util.Optional;

public interface ConfigProperty {

  String getName();

  Optional<String> getSource();

  Optional<String> getOrigin();

  Optional<String> getAsString();

  String getAsString(String defaultValue);
}
