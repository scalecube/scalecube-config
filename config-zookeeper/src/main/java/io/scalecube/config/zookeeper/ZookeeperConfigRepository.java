package io.scalecube.config.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.scalecube.config.keyvalue.KeyValueConfigEntity;
import io.scalecube.config.keyvalue.KeyValueConfigName;
import io.scalecube.config.keyvalue.KeyValueConfigRepository;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ZookeeperConfigRepository implements KeyValueConfigRepository {
  private final ZookeeperConfigConnector connector;

  public ZookeeperConfigRepository(@Nonnull ZookeeperConfigConnector connector) {
    this.connector = Objects.requireNonNull(connector);
  }

  @Override
  public List<KeyValueConfigEntity> findAll(@Nonnull KeyValueConfigName configName) throws Exception {
    String collectionPath = "/" + configName.getQualifiedName();
    byte[] data = connector.getClient().getData(collectionPath, false, null);
    ByteArrayInputStream bin = new ByteArrayInputStream(data);
    ObjectMapper objectMapper = ZookeeperConfigObjectMapper.getInstance();
    ObjectReader objectReader = objectMapper.readerFor(ZookeeperConfigEntity.class);
    return ((ZookeeperConfigEntity) objectReader.readValue(bin)).getConfig().stream()
        .map(input -> input.setConfigName(configName))
        .collect(Collectors.toList());
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
