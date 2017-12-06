package io.scalecube.config.mongo;

import io.scalecube.config.keyvalue.KeyValueConfigEntity;
import io.scalecube.config.keyvalue.KeyValueConfigRepository;
import io.scalecube.config.utils.ThrowableUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import org.bson.RawBsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MongoConfigRepository implements KeyValueConfigRepository {
  private static final Logger LOGGER = LoggerFactory.getLogger(MongoConfigRepository.class);

  private final MongoConfigConnector connector;

  public MongoConfigRepository(@Nonnull MongoConfigConnector connector) {
    this.connector = Objects.requireNonNull(connector);
  }

  @Override
  public List<KeyValueConfigEntity> findAll(@Nullable String groupName, @Nonnull String collectionName) {
    Objects.requireNonNull(collectionName);
    String collectionNameWithGroup = groupName != null ? groupName + '.' + collectionName : collectionName;

    MongoCollection<RawBsonDocument> collection =
        connector.getDatabase().getCollection(collectionNameWithGroup, RawBsonDocument.class);

    MongoCursor<RawBsonDocument> it = collection.find().iterator();
    if (!it.hasNext()) {
      return Collections.emptyList();
    }

    List<KeyValueConfigEntity> result;
    RawBsonDocument document = it.next();
    try {
      ByteArrayInputStream bin = new ByteArrayInputStream(document.getByteBuffer().array());
      ObjectMapper objectMapper = MongoConfigObjectMapper.getInstance();
      ObjectReader objectReader = objectMapper.readerFor(MongoConfigEntity.class);
      result = ((MongoConfigEntity) objectReader.readValue(bin)).getConfig();
    } catch (Exception e) {
      LOGGER.error("Exception at parsing bson to obj, cause: {}", e, e);
      throw ThrowableUtil.propagate(e);
    }

    // set groupName on returned config key-value pairs
    return result.stream().map(input -> new KeyValueConfigEntity(groupName, input)).collect(Collectors.toList());
  }

  private static class MongoConfigEntity {
    private List<KeyValueConfigEntity> config;

    public MongoConfigEntity() {}

    public List<KeyValueConfigEntity> getConfig() {
      return config;
    }

    public void setConfig(List<KeyValueConfigEntity> config) {
      this.config = config;
    }
  }
}
