package io.scalecube.config.examples;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.StringConfigProperty;
import io.scalecube.config.audit.LoggingConfigEventListener;
import io.scalecube.config.source.ClassPathConfigSource;
import io.scalecube.config.source.FileDirectoryConfigSource;
import java.nio.file.Path;
import java.util.function.Predicate;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class ConfigRegistryExample {

  /**
   * Main method of example of using {@link ConfigRegistry}.
   *
   * @param args program arguments
   */
  public static void main(String[] args) {
    Predicate<Path> propsPredicate = path -> path.toString().endsWith(".props");

    String basePath = "config-examples/config";

    ConfigRegistry configRegistry =
        ConfigRegistry.create(
            ConfigRegistrySettings.builder()
                .addLastSource("classpath", new ClassPathConfigSource(propsPredicate))
                .addLastSource(
                    "configDirectory", new FileDirectoryConfigSource(basePath, propsPredicate))
                .addListener(new LoggingConfigEventListener())
                .jmxEnabled(true)
                .jmxMBeanName("config.exporter:name=ConfigRegistry")
                .build());

    StringConfigProperty orderedProp1 = configRegistry.stringProperty("orderedProp1");

    System.out.println("### Matched by first predicate orderedProp1=" + orderedProp1.value().get());
  }
}
