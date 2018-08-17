package io.scalecube.config.http.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.scalecube.config.utils.ObjectMapperHolder;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class ObjectMapperProvider implements ContextResolver<ObjectMapper> {

  @Override
  public ObjectMapper getContext(final Class<?> type) {
    return ObjectMapperHolder.getInstance();
  }
}
