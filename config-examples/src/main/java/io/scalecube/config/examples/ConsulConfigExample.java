package io.scalecube.config.examples;

import com.orbitz.consul.model.kv.ImmutableOperation;
import com.orbitz.consul.model.kv.Operation;
import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.StringConfigProperty;
import io.scalecube.config.audit.Slf4JConfigEventListener;
import io.scalecube.config.consul.ConsulConfigConnector;
import io.scalecube.config.consul.ConsulConfigRepository;
import io.scalecube.config.keyvalue.KeyValueConfigName;
import io.scalecube.config.keyvalue.KeyValueConfigSource;

import java.util.Map;

/**
 * <p>For program properly functioning add some data to zookeeper</p>
 *
 * <p>First, you should have some config path, it's like a
 * [{@link KeyValueConfigName#groupName}]/{@link KeyValueConfigName#collectionName}.
 * This example will be searching for properties in ['/ConsulConfigRootPath', '/group1/ConsulConfigRootPath',
 * '/group2/ConsulConfigRootPath', '/group3/ConsulConfigRootPath'] yours Consul instance.</p>
 * <p>Second, all property names should belong specified above config path and contain some value.
 * This example will be searching for following property paths: </p>
 *  <ul>
 *    <li>/ConsulConfigRootPath/propRoot -> prop_value_root</li>
 *    <li>/group1/ConsulConfigRootPath/prop1 -> prop_value_1</li>
 *    <li>/group2/ConsulConfigRootPath/prop2 -> prop_value_2</li>
 *    <li>/group3/ConsulConfigRootPath/prop3 -> prop_value_3</li>
 *  </ul>
 *  <p>If you want, you can use {@link #init(ConsulConfigConnector)} before to fill your Consul instance</p>
 *  <p>NOTICE: if you are concerned about the consistency of changing your properties use implementation with
 *  a transaction, for example {@link #put(ConsulConfigConnector, Map)}</p>
 */
public class ConsulConfigExample {

  public static void main(String[] args) {
    ConsulConfigConnector connector = ConsulConfigConnector.forUri("http://localhost:8500").build();
    String configSourceCollectionName = "ConsulConfigRootPath";

    ConsulConfigRepository repository = new ConsulConfigRepository(connector);

//    init(repository);

    KeyValueConfigSource consulConfigSource = KeyValueConfigSource
        .withRepository(repository, configSourceCollectionName)
        .groups("group1", "group2", "group3")
        .build();

    ConfigRegistry configRegistry = ConfigRegistry.create(
        ConfigRegistrySettings.builder()
            .addLastSource("ConsulConfig", consulConfigSource)
            .addListener(new Slf4JConfigEventListener())
            .keepRecentConfigEvents(3)
            .reloadIntervalSec(1)
            .build());

    System.out.println(configRegistry.allProperties());

    StringConfigProperty prop1 = configRegistry.stringProperty("prop1");
    System.out.println("### Initial consul config property: prop1=" + prop1.value().get() +
        ", group=" + prop1.origin().get());

    StringConfigProperty prop2 = configRegistry.stringProperty("prop2");
    System.out.println("### Initial consul config property: prop2=" + prop2.value().get() +
        ", group=" + prop2.origin().get());

    StringConfigProperty prop3 = configRegistry.stringProperty("prop3");
    System.out.println("### Initial consul config property: prop3=" + prop3.value().get() +
        ", group=" + prop3.origin().get());

    StringConfigProperty propRoot = configRegistry.stringProperty("propRoot");
    System.out.println("### Initial consul config **root** property: propRoot=" + propRoot.value().get() +
        ", group=" + propRoot.origin().get());

  }

  private static void init(ConsulConfigConnector connector) {
    put(connector, "/group1/ConsulConfigRootPath/prop1", "prop_value_1");
    put(connector, "/group2/ConsulConfigRootPath/prop2", "prop_value_2");
    put(connector, "/group3/ConsulConfigRootPath/prop3", "prop_value_3");
    put(connector, "/ConsulConfigRootPath/propRoot", "prop_value_Root");
  }

  private static void put(ConsulConfigConnector connector, String path, String value) {
    connector.getClient().putValue(path, value);
  }

  private static void put(ConsulConfigConnector connector, Map<String, String> props) {
    Operation[] operations = props.entrySet().stream()
        .map(entry -> ImmutableOperation.builder()
            .key(entry.getKey())
            .value(entry.getValue())
            .build())
        .toArray(Operation[]::new);
    connector.getClient().performTransaction(operations);
  }
}
