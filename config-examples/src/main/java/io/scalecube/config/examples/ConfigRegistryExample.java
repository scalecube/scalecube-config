package io.scalecube.config.examples;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.StringConfigProperty;
import io.scalecube.config.audit.Slf4JConfigEventListener;
import io.scalecube.config.http.server.ConfigRegistryHttpServer;
import io.scalecube.config.source.ClassPathConfigSource;
import io.scalecube.config.source.DirectoryConfigSource;

import java.nio.file.Path;
import java.util.function.Predicate;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class ConfigRegistryExample {

  public static void main(String[] args) {
    Predicate<Path> propsPredicate = path -> path.toString().endsWith(".props");

    String basePath = "config-examples/config";

    ConfigRegistry configRegistry = ConfigRegistry.create(
        ConfigRegistrySettings.builder()
            .addLastSource("classpath", new ClassPathConfigSource(propsPredicate))
            .addLastSource("configDirectory", new DirectoryConfigSource(basePath, propsPredicate))
            .addListener(new Slf4JConfigEventListener())
            .jmxEnabled(true)
            .jmxMBeanName("config.exporter:name=ConfigRegistry")
            .build());

    StringConfigProperty orderedProp1 = configRegistry.stringProperty("orderedProp1");

    System.out.println("### Matched by first predicate orderedProp1=" + orderedProp1.get().get());

    // Start REST HTTP Server
    ConfigRegistryHttpServer.create(configRegistry, 5050);
  }
}
