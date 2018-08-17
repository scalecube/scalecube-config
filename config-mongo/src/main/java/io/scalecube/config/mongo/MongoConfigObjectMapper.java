package io.scalecube.config.mongo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.undercouch.bson4jackson.BsonFactory;
import de.undercouch.bson4jackson.BsonModule;
import de.undercouch.bson4jackson.BsonParser;

public class MongoConfigObjectMapper {

  private static final ObjectMapper objectMapper =
      new ObjectMapper(new BsonFactory().enable(BsonParser.Feature.HONOR_DOCUMENT_LENGTH));

  static {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(
        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); // gives mongo type date in db
    objectMapper.registerModule(new BsonModule()); // gives mongo types in db
  }

  private MongoConfigObjectMapper() {
    // Do not instantiate
  }

  public static ObjectMapper getInstance() {
    return objectMapper;
  }
}
