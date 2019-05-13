package io.scalecube.config.service;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.source.SystemPropertiesConfigSource;
import io.scalecube.config.utils.ObjectMapperHolder;
import io.scalecube.configuration.api.Acknowledgment;
import io.scalecube.configuration.api.ConfigurationService;
import io.scalecube.configuration.api.FetchResponse;
import io.scalecube.configuration.api.SaveRequest;
import io.scalecube.test.fixtures.Fixture;
import java.util.ArrayList;
import java.util.List;
import org.opentest4j.TestAbortedException;
import reactor.core.publisher.Mono;

public class LocalMockServiceFixture implements Fixture {

  private ConfigurationService service;
  private ConfigRegistry configRegistry;

  @Override
  public void setUp() throws TestAbortedException {

    List<FetchResponse> responses = new ArrayList<>();
    Acknowledgment acknowledgment = new Acknowledgment();
    service = mock(ConfigurationService.class);
    when(service.entries(any()))
        .then(
            answer -> {
              return Mono.just(responses);
            });
    when(service.save(any()))
        .then(
            answer -> {
              SaveRequest request = (SaveRequest) answer.getArguments()[0];
              String value =
                  ObjectMapperHolder.getInstance()
                      .writeValueAsString(request.value());
              FetchResponse response = new FetchResponse(request.key(), value);
              responses.add(response);
              return Mono.just(acknowledgment);
            });

    configRegistry =
        ConfigRegistry.create(
            ConfigRegistrySettings.builder()
                .addFirstSource("System", new SystemPropertiesConfigSource())
                .addLastSource(
                    "ScalecubeConfigurationService",
                    new ScalecubeConfigurationServiceConfigSource(BrokerData.class, service))
                .reloadIntervalSec(3)
                .build());
  }

  @Override
  public <T> T proxyFor(Class<? extends T> clasz) {
    if (clasz.isAssignableFrom(ConfigurationService.class)) {
      return clasz.cast(service);
    } else if (clasz.isAssignableFrom(ConfigRegistry.class)) {
      return clasz.cast(configRegistry);
    }
    return null;
  }

  @Override
  public void tearDown() {
    // nothing to do here
  }
}
