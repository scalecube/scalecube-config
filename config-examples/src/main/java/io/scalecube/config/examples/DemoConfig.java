package io.scalecube.config.examples;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.StringConfigProperty;
import io.scalecube.config.audit.LoggingConfigEventListener;
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

    // Local resource cfg source init
    Predicate<Path> propsPredicate = path -> path.toString().endsWith(".props");
    String basePath = "config-examples/config";

    // Config registry init
    ConfigRegistry configRegistry =
        ConfigRegistry.create(
            ConfigRegistrySettings.builder()
                .addLastSource(
                    "ConfigDirectory", new FileDirectoryConfigSource(basePath, propsPredicate))
                .addListener(new LoggingConfigEventListener())
                .keepRecentConfigEvents(10)
                .reloadIntervalSec(3)
                .jmxEnabled(true)
                .jmxMBeanName("config.exporter:name=ConfigRegistry")
                .build());

    // Inject cfgReg into target component
    SomeComponent component = new SomeComponent(configRegistry);
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
