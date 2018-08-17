package io.scalecube.config.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import io.scalecube.config.keyvalue.KeyValueConfigEntity;
import io.scalecube.config.keyvalue.KeyValueConfigName;
import io.scalecube.config.keyvalue.KeyValueConfigRepository;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.bson.RawBsonDocument;

public class MongoConfigRepository implements KeyValueConfigRepository {
  private final MongoConfigConnector connector;

  public MongoConfigRepository(@Nonnull MongoConfigConnector connector) {
    this.connector = Objects.requireNonNull(connector);
  }

  @Override
  public List<KeyValueConfigEntity> findAll(@Nonnull KeyValueConfigName configName)
      throws Exception {
    Objects.requireNonNull(configName);

    String collectionName = configName.getQualifiedName();
    MongoCollection<RawBsonDocument> collection =
        connector.getDatabase().getCollection(collectionName, RawBsonDocument.class);

    MongoCursor<RawBsonDocument> it = collection.find().iterator();
    if (!it.hasNext()) {
      return Collections.emptyList();
    }

    RawBsonDocument document = it.next();
    ByteArrayInputStream bin = new ByteArrayInputStream(document.getByteBuffer().array());
    ObjectMapper objectMapper = MongoConfigObjectMapper.getInstance();
    ObjectReader objectReader = objectMapper.readerFor(MongoConfigEntity.class);
    List<KeyValueConfigEntity> result =
        ((MongoConfigEntity) objectReader.readValue(bin)).getConfig();

    // set groupName on returned config key-value pairs
    return result
        .stream()
        .map(input -> input.setConfigName(configName))
        .collect(Collectors.toList());
  }

  // Helper class for mapping bson document
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
