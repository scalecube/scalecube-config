package io.scalecube.config.consul;

import io.scalecube.config.keyvalue.KeyValueConfigEntity;
import io.scalecube.config.keyvalue.KeyValueConfigName;
import io.scalecube.config.keyvalue.KeyValueConfigRepository;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ConsulConfigRepository implements KeyValueConfigRepository {
  private final ConsulConfigConnector connector;

  public ConsulConfigRepository(@Nonnull ConsulConfigConnector connector) {
    this.connector = Objects.requireNonNull(connector);
  }

  @Override
  public List<KeyValueConfigEntity> findAll(@Nonnull KeyValueConfigName configName) {
    String prefix = resolvePath(configName);
    return connector.getClient().getValues(prefix).stream()
        .filter(entry -> entry.getValue().isPresent())
        .map(entry -> {
          KeyValueConfigEntity entity = new KeyValueConfigEntity().setConfigName(configName);
          entity.setPropName(entry.getKey().substring(prefix.length()).replaceAll("/", "."));
          entity.setPropValue(entry.getValueAsString().get());
          return entity;
        }).collect(Collectors.toList());
  }

  private String resolvePath(KeyValueConfigName configName) {
    String path = configName.getGroupName().orElse("");
    if (!path.isEmpty() && !path.endsWith("/")) {
      path += "/";
    }
    path += configName.getCollectionName();
    if (!path.isEmpty() && !path.endsWith("/")) {
      path += "/";
    }
    return path;
  }
}
