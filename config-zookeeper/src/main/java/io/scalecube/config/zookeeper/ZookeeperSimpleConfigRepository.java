package io.scalecube.config.zookeeper;

import io.scalecube.config.keyvalue.KeyValueConfigEntity;
import io.scalecube.config.keyvalue.KeyValueConfigName;
import io.scalecube.config.keyvalue.KeyValueConfigRepository;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ZookeeperSimpleConfigRepository implements KeyValueConfigRepository {

  private final CuratorFramework client;

  public ZookeeperSimpleConfigRepository(@Nonnull ZookeeperConfigConnector connector) {
    this(Objects.requireNonNull(connector).getClient());
  }

  public ZookeeperSimpleConfigRepository(@Nonnull CuratorFramework client) {
    this.client = Objects.requireNonNull(client);
  }

  @Override
  public List<KeyValueConfigEntity> findAll(@Nonnull KeyValueConfigName configName) throws Exception {
    List<KeyValueConfigEntity> entities = new ArrayList<>();
    String path = resolvePath(configName);
    List<String> children = client.getChildren().forPath(path);
    for (String child : children) {
      findAll(configName, ZKPaths.makePath(path, child), entities);
    }
    return entities;
  }

  private void findAll(KeyValueConfigName configName, String path, List<KeyValueConfigEntity> entities) throws Exception {
    byte[] data = client.getData().forPath(path);
    if (data != null && data.length > 0) {
      String value = new String(data, StandardCharsets.UTF_8);
      KeyValueConfigEntity entity = new KeyValueConfigEntity().setConfigName(configName);
      entity.setPropName(propertyName(configName, path));
      entity.setPropValue(value);
      entities.add(entity);
    }
    List<String> children = client.getChildren().forPath(path);
    for (String child : children) {
      findAll(configName, ZKPaths.makePath(path, child), entities);
    }
  }

  private String resolvePath(KeyValueConfigName configName) {
    return configName.getGroupName().map(group -> "/" + group + "/").orElse("/") + configName.getCollectionName();
  }

  private String propertyName(KeyValueConfigName configName, String fullPath) {
    String rootPath = resolvePath(configName);
    if (rootPath.equals(fullPath)) {
      return "";
    } else {
      return fullPath.substring(rootPath.length() + "/".length()).replace("/", ".");
    }
  }
}
