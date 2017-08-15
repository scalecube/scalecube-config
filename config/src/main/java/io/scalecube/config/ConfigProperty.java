package io.scalecube.config;

import java.util.Optional;

public interface ConfigProperty {

	Optional<String> getSource();

	Optional<String> getOrigin();

	String getName();

	Optional<String> getAsString();

	String getAsString(String defaultValue);
}
