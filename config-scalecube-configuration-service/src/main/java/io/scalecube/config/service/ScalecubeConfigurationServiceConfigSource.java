package io.scalecube.config.service;

import static io.scalecube.services.gateway.clientsdk.Client.http;
import static io.scalecube.services.gateway.clientsdk.ClientSettings.builder;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigSourceNotAvailableException;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.LoadedConfigProperty;
import io.scalecube.configuration.api.ConfigurationService;
import io.scalecube.configuration.api.EntriesRequest;
import io.scalecube.configuration.api.FetchResponse;
import io.scalecube.services.gateway.clientsdk.ClientMessage;
import io.scalecube.services.transport.jackson.JacksonCodec;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
          .fetchAll(requestEntries)
          .collectMap(FetchResponse::key, Parsing::fromFetchResponse)
          .block();
    } catch (Exception e) {
      e.printStackTrace();
      LOGGER.warn("unable to load config properties", e);
      throw new ConfigSourceNotAvailableException(e);
    }
  }

  static class Parsing {
    static ConfigProperty fromFetchResponse(FetchResponse fetchResponse) {
      return LoadedConfigProperty.withNameAndValue(
              fetchResponse.key(), fetchResponse.value().toString())
          .build();
    }
  }
}
