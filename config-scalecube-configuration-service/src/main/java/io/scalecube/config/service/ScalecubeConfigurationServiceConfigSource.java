package io.scalecube.config.service;

import static io.scalecube.services.gateway.clientsdk.Client.http;
import static io.scalecube.services.gateway.clientsdk.ClientSettings.builder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigSourceNotAvailableException;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.LoadedConfigProperty;
import io.scalecube.config.utils.ObjectMapperHolder;
import io.scalecube.configuration.api.ConfigurationService;
import io.scalecube.configuration.api.EntriesRequest;
import io.scalecube.configuration.api.FetchResponse;
import io.scalecube.services.annotations.Service;
import io.scalecube.services.annotations.ServiceMethod;
import io.scalecube.services.transport.jackson.JacksonCodec;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.HttpResources;

public class ScalecubeConfigurationServiceConfigSource implements ConfigSource {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ScalecubeConfigurationServiceConfigSource.class);

  private ConfigurationService service;

  private EntriesRequest requestEntries;

  /**
   * Create a configuration source that connects to the production environment of scalecube
   * configuration service.
   *
   * @param token the API token
   * @param repository the name of the repository
   */
  public ScalecubeConfigurationServiceConfigSource(String token, String repository) {
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
          .collectMap(FetchResponse::key, Parsing::fromFetchResponse)
          .block();
    } catch (Exception e) {
      e.printStackTrace();
      LOGGER.warn("unable to load config properties", e);
      throw new ConfigSourceNotAvailableException(e);
    }
  }

  static class Parsing {
    private static ObjectWriter writer = ObjectMapperHolder.getInstance().writer(new MinimalPrettyPrinter());

    static ConfigProperty fromFetchResponse(FetchResponse fetchResponse) {
      try {
        return LoadedConfigProperty.withNameAndValue(
                fetchResponse.key(), writer.writeValueAsString(fetchResponse.value()))
            .build();
      } catch (JsonProcessingException ignoredException) {
        return LoadedConfigProperty.withNameAndValue(
                fetchResponse.key(), fetchResponse.value().toString())
            .build();
      }
    }
  }
}
