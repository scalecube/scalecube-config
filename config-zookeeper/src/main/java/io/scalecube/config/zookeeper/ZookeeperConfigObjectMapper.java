package io.scalecube.config.zookeeper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ZookeeperConfigObjectMapper {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  private ZookeeperConfigObjectMapper() {
    // Do not instantiate
  }

  public static ObjectMapper getInstance() {
    return objectMapper;
  }
}
