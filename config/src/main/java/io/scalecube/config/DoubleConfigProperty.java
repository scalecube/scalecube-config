package io.scalecube.config;

import java.util.Optional;
import java.util.function.BiConsumer;

public interface DoubleConfigProperty extends ConfigProperty {

	Optional<Double> get();

	double get(double defaultValue);

	void addCallback(BiConsumer<Double, Double> callback);
}
