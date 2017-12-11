package io.scalecube.config.examples;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.StringConfigProperty;
import io.scalecube.config.audit.Slf4JConfigEventListener;
import io.scalecube.config.keyvalue.KeyValueConfigSource;
import io.scalecube.config.mongo.MongoConfigConnector;
import io.scalecube.config.mongo.MongoConfigEventListener;
import io.scalecube.config.mongo.MongoConfigRepository;

/**
 * For program properly functioning add some data to mogno.
 * <ul>
 * <li>Collection {@code group1.config_source} must have: {@code prop1->value-from-group1}</li>
 * <li>Collection {@code group2.config_source} must have: {@code prop1->value-from-group2}</li>
 * <li>Collection {@code group3.config_source} must have: {@code prop2->value-from-group3}</li>
 * <li>Collection {@code config_source} must have: {@code propRoot->value-from-root}</li>
 * </ul>
 * <p/>
 * <b>NOTE:</b> A document in certain collection comes in format:
 * 
 * <pre>
 *   {
 *     "config" :
 *     [ {
 *         "propName" : "prop_name_1",
 *         "propValue" : "prop_value_1"
 *       },
 *       ...
 *       {
 *         "propName" : "prop_name_N",
 *         "propValue" : "prop_value_N"
 *       }
 *     ]
 *   }
 * </pre>
 */
public class MongoConfigExample {

  public static void main(String[] args) throws Exception {
    String databaseName = args[0] != null ? args[0] : "MongoConfigExample" + System.currentTimeMillis();
    String uri = "mongodb://localhost:27017/" + databaseName;
    String configSourceCollectionName = "MongoConfigRepository";
    String auditLogCollectionName = "TestConfigurationAuditLog";

    MongoConfigConnector connector = MongoConfigConnector.builder().forUri(uri).build();

    KeyValueConfigSource mongoConfigSource = KeyValueConfigSource
        .withRepository(new MongoConfigRepository(connector), configSourceCollectionName)
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
}
