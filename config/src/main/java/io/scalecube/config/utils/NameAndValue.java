package io.scalecube.config.utils;

import java.util.Optional;

public class NameAndValue {
  private final String name;
  private final String value;

  public NameAndValue(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public Optional<String> getValue() {
    return Optional.ofNullable(value);
  }

  @Override
  public String toString() {
    return "NameAndValue{" +
        "name='" + name + '\'' +
        ", value='" + value + '\'' +
        '}';
  }
}
