package io.scalecube.config.service;

import static io.scalecube.services.gateway.clientsdk.Client.http;
import static io.scalecube.services.gateway.clientsdk.ClientSettings.builder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigSourceNotAvailableException;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.LoadedConfigProperty;
import io.scalecube.config.source.LoadedObjectConfigProperty;
import io.scalecube.config.utils.ObjectMapperHolder;
import io.scalecube.configuration.api.ConfigurationService;
import io.scalecube.configuration.api.EntriesRequest;
import io.scalecube.configuration.api.FetchResponse;
import io.scalecube.services.transport.jackson.JacksonCodec;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.http.HttpResources;

public class ScalecubeConfigurationServiceConfigSource implements ConfigSource {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ScalecubeConfigurationServiceConfigSource.class);

  private ConfigurationService service;

  private EntriesRequest requestEntries;

  private Parsing<?> parsing;

  /**
   * Create a configuration source that connects to the production environment of scalecube
   * configuration service.
   *
   * @param token the API token
   * @param repository the name of the repository
   */
  public ScalecubeConfigurationServiceConfigSource(String token, String repository) {
    this(token, repository, Object.class);
  }

  /**
   * Create a configuration source that connects to the production environment of scalecube
   * configuration service.
   *
   * @param token the API token
   * @param repository the name of the repository
   */
  public ScalecubeConfigurationServiceConfigSource(
      String token, String repository, Class<?> schema) {
    this.parsing = new Parsing<>(schema);
    this.service =
        http(builder()
                .host("configuration-service-http.genesis.om2.com")
                .port(443)
                .secure()
                .contentType(JacksonCodec.CONTENT_TYPE)
                .loopResources(HttpResources.get())
                .build())
            .forService(ConfigurationService.class);
    requestEntries = new EntriesRequest(token, repository);
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    try {
      return service
          .entries(requestEntries)
          .flatMapIterable(Function.identity())
          .collectMap(FetchResponse::key, this.parsing::fromFetchResponse)
          .block();
    } catch (Exception e) {
      e.printStackTrace();
      LOGGER.warn("unable to load config properties", e);
      throw new ConfigSourceNotAvailableException(e);
    }
  }

  static class Parsing<T> {
    private Class<T> schema;
    private ObjectWriter writer;

    protected Parsing(Class<T> schema) {
      this.schema = schema;
      writer = ObjectMapperHolder.getInstance().writer(new MinimalPrettyPrinter()).forType(schema);
    }

    ConfigProperty fromFetchResponse(FetchResponse fetchResponse) {
      if (schema.equals(Object.class)) {
        try {
          return LoadedConfigProperty.withNameAndValue(
                  fetchResponse.key(), writer.writeValueAsString(fetchResponse.value()))
              .build();
        } catch (JsonProcessingException ignoredException) {
          return LoadedConfigProperty.withNameAndValue(
                  fetchResponse.key(), fetchResponse.value().toString())
              .build();
        }
      } else {
        T value = ObjectMapperHolder.getInstance().convertValue(fetchResponse.value(), schema);
        return LoadedObjectConfigProperty.forNameAndValue(fetchResponse.key(), value);
      }
    }
  }
}
