package io.scalecube.config.examples;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.StringConfigProperty;
import io.scalecube.config.audit.Slf4JConfigEventListener;
import io.scalecube.config.keyvalue.KeyValueConfigName;
import io.scalecube.config.keyvalue.KeyValueConfigRepository;
import io.scalecube.config.keyvalue.KeyValueConfigSource;
import io.scalecube.config.utils.ThrowableUtil;
import io.scalecube.config.zookeeper.ZookeeperConfigConnector;
import io.scalecube.config.zookeeper.ZookeeperJsonConfigRepository;
import io.scalecube.config.zookeeper.ZookeeperSimpleConfigRepository;
import io.scalecube.config.zookeeper.cache.ZookeeperCallbackCacheConfigRepository;
import io.scalecube.config.zookeeper.cache.ZookeeperScheduledCacheConfigRepository;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>For program properly functioning add some data to zookeeper</p>
 *
 * <p>First, you should have some config path, it's like a
 * [{@link KeyValueConfigName#groupName}]/{@link KeyValueConfigName#collectionName}.
 * This example will be searching for properties in ['/ZookeeperConfigRootPath', '/group1/ZookeeperConfigRootPath',
 * '/group2/ZookeeperConfigRootPath', '/group3/ZookeeperConfigRootPath'] yours Zookeeper instance.</p>
 * <p>Second, all property names should belong specified above config path and contain some value.
 * This example will be searching for following property paths: </p>
 *  <ul>
 *    <li>/ZookeeperConfigRootPath/propRoot -> prop_value_root</li>
 *    <li>/ZookeeperConfigRootPath/propRoot/sub/sub2 -> prop_value_SubRoot</li>
 *    <li>/group1/ZookeeperConfigRootPath/prop1 -> prop_value_1</li>
 *    <li>/group2/ZookeeperConfigRootPath/prop2 -> prop_value_2</li>
 *    <li>/group3/ZookeeperConfigRootPath/prop3 -> prop_value_3</li>
 *  </ul>
 *  <p>If you want, you can use {@link #init(ZookeeperConfigConnector)} before to fill your Zookeeper instance</p>
 *  <p>NOTICE: if you are concerned about the consistency of changing your properties use implementation with
 *  a transaction, for example {@link #put(ZookeeperConfigConnector, Map)}</p>
 */
public class ZookeeperConfigExample {

  public static void main(String[] args) throws Exception {
    ZookeeperConfigConnector connector = ZookeeperConfigConnector.forUri("localhost:2181").build();
    String configSourceCollectionName = "ZookeeperConfigRootPath";

//    KeyValueConfigRepository repository = new ZookeeperSimpleConfigRepository(connector);
    KeyValueConfigRepository repository = new ZookeeperCallbackCacheConfigRepository(connector);
//    KeyValueConfigRepository repository = new ZookeeperScheduledCacheConfigRepository(connector, Duration.ofSeconds(2));
//    KeyValueConfigRepository repository = new ZookeeperJsonConfigRepository(connector);

    init(connector);

    KeyValueConfigSource zookeeperConfigSource = KeyValueConfigSource
        .withRepository(repository, configSourceCollectionName)
        .groups("group1", "group2", "group3")
        .build();

    ConfigRegistry configRegistry = ConfigRegistry.create(
        ConfigRegistrySettings.builder()
            .addLastSource("ZookeeperConfig", zookeeperConfigSource)
            .addListener(new Slf4JConfigEventListener())
            .keepRecentConfigEvents(3)
            .reloadIntervalSec(1)
            .build());

    StringConfigProperty prop1 = configRegistry.stringProperty("prop1");
    System.out.println("### Initial zookeeper config property: prop1=" + prop1.value().get() +
        ", group=" + prop1.origin().get());

    StringConfigProperty prop2 = configRegistry.stringProperty("prop2");
    System.out.println("### Initial zookeeper config property: prop2=" + prop2.value().get() +
        ", group=" + prop2.origin().get());

    StringConfigProperty prop3 = configRegistry.stringProperty("prop3");
    System.out.println("### Initial zookeeper config property: prop3=" + prop3.value().get() +
        ", group=" + prop3.origin().get());

    StringConfigProperty propRoot = configRegistry.stringProperty("propRoot");
    System.out.println("### Initial zookeeper config **root** property: propRoot=" + propRoot.value().get() +
        ", group=" + propRoot.origin().get());

    StringConfigProperty propSubRoot = configRegistry.stringProperty("propRoot.sub.sub2");
    System.out.println("### Initial zookeeper config property: propRoot.sub1.sub2=" + propSubRoot.value().get() +
        ", group=" + propSubRoot.origin().get());

    TimeUnit.SECONDS.sleep(3);
  }

  private static void init(ZookeeperConfigConnector connector) throws Exception {
    put(connector,"/group1/ZookeeperConfigRootPath/prop1", "prop_value_1");
    put(connector,"/group2/ZookeeperConfigRootPath/prop2", "prop_value_2");
    put(connector,"/group3/ZookeeperConfigRootPath/prop3", "prop_value_3");
    put(connector,"/ZookeeperConfigRootPath/propRoot", "prop_value_Root");
    put(connector,"/ZookeeperConfigRootPath/propRoot/sub/sub2", "prop_value_SubRoot");
    initJson(connector);
  }

  private static void initJson(ZookeeperConfigConnector connector) throws Exception {
    put(connector, "/ZookeeperConfigRootPath", "{\"config\": [{\"propName\": \"propRoot\",\"propValue\": \"prop_value_Root\"},{\"propName\": \"propRoot.sub.sub2\",\"propValue\": \"prop_value_SubRoot\"}]}");
    put(connector, "/group1/ZookeeperConfigRootPath", "{\"config\": [{\"propName\": \"prop1\",\"propValue\": \"prop_value_1\"}]}");
    put(connector, "/group2/ZookeeperConfigRootPath", "{\"config\": [{\"propName\": \"prop2\",\"propValue\": \"prop_value_2\"}]}");
    put(connector, "/group3/ZookeeperConfigRootPath", "{\"config\": [{\"propName\": \"prop3\",\"propValue\": \"prop_value_3\"}]}");
  }

  private static void put(ZookeeperConfigConnector connector, String path, String value) throws Exception {
    byte[] data = value.getBytes(StandardCharsets.UTF_8);
    try {
      connector.getClient().create().creatingParentsIfNeeded().forPath(path, data);
    } catch (KeeperException.NodeExistsException e) {
      // key already exists - update the data instead
      connector.getClient().setData().forPath(path, data);
    }
  }

  private static void put(ZookeeperConfigConnector connector, Map<String, String> props) throws Exception {
    CuratorFramework client = connector.getClient();

    client.transaction().forOperations(props.entrySet().stream()
        .map(entry -> {
          try {
            String path = entry.getKey();
            byte[] data = entry.getValue().getBytes(StandardCharsets.UTF_8);
//            client.create().creatingParentsIfNeeded().forPath(path);
            return client.transactionOp().setData().forPath(path, data);
          } catch (Exception e) {
            throw ThrowableUtil.propagate(e);
          }
        }).collect(Collectors.toList()));
  }
}
