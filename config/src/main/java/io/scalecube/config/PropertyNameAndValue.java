package io.scalecube.config;

import java.util.Optional;

/**
 * Helper tuple class. Stores pair of property name and property value.
 */
class PropertyNameAndValue {
  private final String name;
  private final String value;

  PropertyNameAndValue(String name, String value) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("PropertyNameAndValue: property name can't be null or empty");
    }
    this.name = name;
    this.value = value;
  }

  String getName() {
    return name;
  }

  Optional<String> getValue() {
    return Optional.ofNullable(value);
  }

  @Override
  public String toString() {
    return "PropertyNameAndValue{" +
        "name='" + name + '\'' +
        ", value='" + value + '\'' +
        '}';
  }
}
