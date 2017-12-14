package io.scalecube.config.examples;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.StringConfigProperty;
import io.scalecube.config.audit.Slf4JConfigEventListener;
import io.scalecube.config.keyvalue.KeyValueConfigSource;
import io.scalecube.config.zookeeper.ZookeeperConfigConnector;
import io.scalecube.config.zookeeper.ZookeeperConfigEventListener;
import io.scalecube.config.zookeeper.ZookeeperConfigRepository;

/**
 * For program properly functioning add some data to zookeeper.
 * <ul>
 * <li>Collection {@code group1.config_source} must have: {@code prop1->value-from-group1}</li>
 * <li>Collection {@code group2.config_source} must have: {@code prop1->value-from-group2}</li>
 * <li>Collection {@code group3.config_source} must have: {@code prop2->value-from-group3}</li>
 * <li>Collection {@code config_source} must have: {@code propRoot->value-from-root}</li>
 * </ul>
 * <p/>
 * <b>NOTE:</b> A document in certain collection comes in format:
 * <p>
 * <pre>
 *   {
 *     "config" :
 *     [ {
 *         "propName" : "prop1",
 *         "propValue" : "prop_value_1"
 *       },
 *       {
 *         "propName" : "prop2",
 *         "propValue" : "prop_value_2"
 *       },
 *       ...
 *       {
 *         "propName" : "propRoot",
 *         "propValue" : "prop_value_N"
 *       }
 *     ]
 *   }
 * </pre>
 */
public class ZookeeperConfigExample {

  public static void main(String[] args) throws Exception {
    ZookeeperConfigConnector connector = ZookeeperConfigConnector.Builder.forUri("192.168.99.100:2181").build();
    String configSourceCollectionName = "ZookeeperConfigRepository";
    String auditLogCollectionName = "TestConfigurationAuditLog";

    KeyValueConfigSource zookeeperConfigSource = KeyValueConfigSource
        .withRepository(new ZookeeperConfigRepository(connector), configSourceCollectionName)
        .groups("group1", "group2", "group3")
        .build();

    ConfigRegistry configRegistry = ConfigRegistry.create(
        ConfigRegistrySettings.builder()
            .addLastSource("ZookeeperConfig", zookeeperConfigSource)
            .addListener(new Slf4JConfigEventListener())
            .addListener(new ZookeeperConfigEventListener(connector, auditLogCollectionName))
            .keepRecentConfigEvents(3)
            .reloadIntervalSec(1)
            .build());

    StringConfigProperty prop1 = configRegistry.stringProperty("prop1");
    System.out.println("### Initial zookeeper config property: prop1=" + prop1.value().get() +
        ", group=" + prop1.origin().get());

    StringConfigProperty prop2 = configRegistry.stringProperty("prop2");
    System.out.println("### Initial zookeeper config property: prop2=" + prop2.value().get() +
        ", group=" + prop2.origin().get());

    StringConfigProperty propRoot = configRegistry.stringProperty("propRoot");
    System.out.println("### Initial zookeeper config **root** property: propRoot=" + propRoot.value().get() +
        ", group=" + propRoot.origin().get());
  }
}
