package io.scalecube.config.examples;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.StringConfigProperty;
import io.scalecube.config.audit.Slf4JConfigEventListener;
import io.scalecube.config.http.server.ConfigRegistryHttpServer;
import io.scalecube.config.keyvalue.KeyValueConfigSource;
import io.scalecube.config.mongo.MongoConfigConnector;
import io.scalecube.config.mongo.MongoConfigEventListener;
import io.scalecube.config.mongo.MongoConfigRepository;
import io.scalecube.config.source.FileDirectoryConfigSource;
import java.nio.file.Path;
import java.util.function.Predicate;

public class DemoConfig {

  /**
   * Main method of example.
   *
   * @param args program arguments
   */
  public static void main(String[] args) {

    // Mongo property source init
    String databaseName = "MongoConfigExample";
    String uri = "mongodb://localhost:27017/" + databaseName;
    String configSourceCollectionName = "MongoConfigRepository";
    String auditLogCollectionName = "TestConfigurationAuditLog";

    MongoConfigConnector connector = MongoConfigConnector.builder().forUri(uri).build();

    KeyValueConfigSource mongoConfigSource =
        KeyValueConfigSource.withRepository(
                new MongoConfigRepository(connector), configSourceCollectionName)
            .groups("group2", "group1", "root")
            .build();

    // Local resource cfg source init
    Predicate<Path> propsPredicate = path -> path.toString().endsWith(".props");
    String basePath = "config-examples/config";

    // Config registry init
    ConfigRegistry configRegistry =
        ConfigRegistry.create(
            ConfigRegistrySettings.builder()
                .addLastSource(
                    "ConfigDirectory", new FileDirectoryConfigSource(basePath, propsPredicate))
                .addLastSource("MongoConfig", mongoConfigSource)
                .addListener(new Slf4JConfigEventListener())
                .addListener(new MongoConfigEventListener(connector, auditLogCollectionName))
                .keepRecentConfigEvents(10)
                .reloadIntervalSec(3)
                .jmxEnabled(true)
                .jmxMBeanName("config.exporter:name=ConfigRegistry")
                .build());

    // Inject cfgReg into target component
    SomeComponent component = new SomeComponent(configRegistry);

    // Start REST HTTP Server
    ConfigRegistryHttpServer.create(configRegistry, 5050);
  }

  static class SomeComponent {

    private StringConfigProperty host;

    SomeComponent(ConfigRegistry cfgReg) {
      host = cfgReg.stringProperty("host");
      host.addCallback(
          (oldVal, newVal) -> System.out.println("###Property changed: " + oldVal + "->" + newVal));
    }
  }
}
