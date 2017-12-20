package io.scalecube.config.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.scalecube.config.keyvalue.KeyValueConfigEntity;
import io.scalecube.config.keyvalue.KeyValueConfigName;
import io.scalecube.config.keyvalue.KeyValueConfigRepository;
import org.apache.curator.framework.CuratorFramework;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ZookeeperJsonConfigRepository implements KeyValueConfigRepository {

  private final CuratorFramework client;

  public ZookeeperJsonConfigRepository(@Nonnull ZookeeperConfigConnector connector) {
    this(Objects.requireNonNull(connector).getClient());
  }

  public ZookeeperJsonConfigRepository(@Nonnull CuratorFramework client) {
    this.client = Objects.requireNonNull(client);
  }

  @Override
  public List<KeyValueConfigEntity> findAll(@Nonnull KeyValueConfigName configName) throws Exception {
    byte[] data = client.getData().forPath(resolvePath(configName));
    ByteArrayInputStream bin = new ByteArrayInputStream(data);
    ObjectMapper objectMapper = ZookeeperConfigObjectMapper.getInstance();
    ObjectReader objectReader = objectMapper.readerFor(ZookeeperConfigEntity.class);
    return ((ZookeeperConfigEntity) objectReader.readValue(bin)).getConfig().stream()
        .map(input -> input.setConfigName(configName))
        .collect(Collectors.toList());
  }

  private String resolvePath(KeyValueConfigName configName) {
    return configName.getGroupName().map(group -> "/" + group + "/").orElse("/") + configName.getCollectionName();
  }

  private static class ZookeeperConfigEntity {
    private List<KeyValueConfigEntity> config;

    public ZookeeperConfigEntity() {
    }

    public List<KeyValueConfigEntity> getConfig() {
      return config;
    }

    public void setConfig(List<KeyValueConfigEntity> config) {
      this.config = config;
    }
  }
}
