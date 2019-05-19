package io.scalecube.config.http.server;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class ObjectMapperProvider implements ContextResolver<ObjectMapper> {

  @Override
  public ObjectMapper getContext(final Class<?> type) {
    return ObjectMapperHolder.objectMapper;
  }

  /**
   * Holder class for {@link ObjectMapper}.
   *
   * @author Anton Kharenko
   */
  private static class ObjectMapperHolder {

    private static ObjectMapper objectMapper = initMapper();

    private static ObjectMapper initMapper() {
      ObjectMapper mapper =
          new ObjectMapper() //
              .registerModule(new Jdk8Module())
              .registerModule(new JavaTimeModule());
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
      mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
      mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
      mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);

      mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      return mapper;
    }
  }
}
