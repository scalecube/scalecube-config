package io.scalecube.config.examples;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.StringConfigProperty;
import io.scalecube.config.audit.Slf4JConfigEventListener;
import io.scalecube.config.mongo.ConfigurationEntity;
import io.scalecube.config.mongo.MongoConfigConnector;
import io.scalecube.config.mongo.MongoConfigEventListener;
import io.scalecube.config.mongo.MongoConfigRepository;
import io.scalecube.config.mongo.MongoConfigSource;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class MongoConfigExample {

  public static void main(String[] args) throws Exception {
    String databaseName = "MongoConfigExample" + System.currentTimeMillis();
    String uri = "mongodb://localhost:27017/" + databaseName;
    String configSourceCollectionName = "TestConfigurationSource";
    String auditLogCollectionName = "TestConfigurationAuditLog";

    MongoConfigConnector connector = MongoConfigConnector.builder().forUri(uri).build();
    MongoConfigRepository repository = new MongoConfigRepository(connector, configSourceCollectionName);

    populateInitialConfigEntries(repository);

    MongoConfigSource mongoConfigSource = MongoConfigSource.withConnector(connector)
        .collectionName(configSourceCollectionName)
        .groups("group1", "group2", "group3")
        .build();

    ConfigRegistry configRegistry = ConfigRegistry.create(
        ConfigRegistrySettings.builder()
            .addLastSource("MongoConfig", mongoConfigSource)
            .addListener(new Slf4JConfigEventListener())
            .addListener(new MongoConfigEventListener(connector, auditLogCollectionName))
            .keepRecentConfigEvents(3)
            .reloadIntervalSec(1)
            .build());

    StringConfigProperty prop1 = configRegistry.stringProperty("prop1");
    System.out.println("### Initial mongo config property: prop1=" + prop1.value().get() +
        ", group=" + prop1.origin().get());

    StringConfigProperty prop2 = configRegistry.stringProperty("prop2");
    System.out.println("### Initial mongo config property: prop2=" + prop2.value().get() +
        ", group=" + prop2.origin().get());

    StringConfigProperty propRoot = configRegistry.stringProperty("propRoot");
    System.out.println("### Initial mongo config **root** property: propRoot=" + propRoot.value().get() +
        ", group=" + propRoot.origin().get());
  }

  private static void populateInitialConfigEntries(MongoConfigRepository repository) throws Exception {
    ConfigurationEntity configEntity1 = new ConfigurationEntity();
    configEntity1.setPropName("prop1");
    configEntity1.setGroupName("group1");
    configEntity1.setPropValue("value-from-group1");

    ConfigurationEntity configEntity2 = new ConfigurationEntity();
    configEntity2.setPropName("prop1");
    configEntity2.setGroupName("group2");
    configEntity2.setPropValue("value-from-group2");

    ConfigurationEntity configEntity3 = new ConfigurationEntity();
    configEntity3.setPropName("prop2");
    configEntity3.setGroupName("group3");
    configEntity3.setPropValue("value-from-group3");

    ConfigurationEntity configEntity4 = new ConfigurationEntity();
    configEntity4.setPropName("propRoot");
    configEntity4.setPropValue("value-from-root");

    repository.insertOneAsync(configEntity1).get();
    repository.insertOneAsync(configEntity2).get();
    repository.insertOneAsync(configEntity3).get();
    repository.insertOneAsync(configEntity4).get();
  }
}
