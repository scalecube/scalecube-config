package io.scalecube.config.examples;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.StringConfigProperty;
import io.scalecube.config.source.ClassPathConfigSource;

import java.nio.file.Path;
import java.util.function.Predicate;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class PredicateOrderingConfigExample {

  public static void main(String[] args) {
    Predicate<Path> propsPredicate = path -> path.toString().endsWith(".props");
    Predicate<Path> firstPredicate = propsPredicate.and(path -> path.toString().contains("order1"));
    Predicate<Path> secondPredicate = propsPredicate.and(path -> path.toString().contains("order2"));

    ConfigRegistry configRegistry = ConfigRegistry.create(
        ConfigRegistrySettings.builder()
            .addLastSource("classpath", new ClassPathConfigSource(firstPredicate, secondPredicate))
            .build());

    StringConfigProperty orderedProp1 = configRegistry.stringProperty("orderedProp1");

    System.out.println("### Matched by first predicate orderedProp1=" + orderedProp1.value().get());
  }
}
