package io.scalecube.config.examples;

import java.nio.file.Path;
import java.util.function.Predicate;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.source.ClassPathConfigSource;

/**
 * @author Anton Kharenko
 */
public class SourceOrderExample {

  public static void main(String[] args) {
    Predicate<Path> propsPredicate = path -> path.toString().endsWith(".props");
    ConfigRegistry configRegistry = ConfigRegistry.create(
        ConfigRegistrySettings.builder()
            .addLastSource("classpath#1", new ClassPathConfigSource(propsPredicate))
            .addFirstSource("classpath#0", new ClassPathConfigSource(propsPredicate))
            .addLastSource("classpath#3", new ClassPathConfigSource(propsPredicate))
            .addBeforeSource("classpath#3", "classpath#2", new ClassPathConfigSource(propsPredicate))
            .build());

    System.out.println("### Sources: \n" + configRegistry.getConfigSources());
  }

}
