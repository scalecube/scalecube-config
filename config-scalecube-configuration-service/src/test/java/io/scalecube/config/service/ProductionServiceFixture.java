package io.scalecube.config.service;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.StringConfigProperty;
import io.scalecube.config.source.SystemEnvironmentConfigSource;
import io.scalecube.config.source.SystemPropertiesConfigSource;
import io.scalecube.configuration.api.ConfigurationService;
import io.scalecube.test.fixtures.Fixture;
import java.util.NoSuchElementException;
import org.opentest4j.TestAbortedException;

public class ProductionServiceFixture implements Fixture {

  private ConfigurationService service;
  private ConfigRegistry configRegistry;

  @Override
  public void setUp() throws TestAbortedException {

    ConfigRegistry registry =
        ConfigRegistry.create(
            ConfigRegistrySettings.builder()
                .addFirstSource("System", new SystemPropertiesConfigSource())
                .addLastSource("ENV", new SystemEnvironmentConfigSource())
                .build());

    StringConfigProperty token = registry.stringProperty("token");
    StringConfigProperty repository = registry.stringProperty("repository");

    ScalecubeConfigurationServiceConfigSource configSource;
    try {
      configSource =
          new ScalecubeConfigurationServiceConfigSource(
              token.valueOrThrow(), repository.valueOrThrow(), BrokerData.class);
    } catch (NoSuchElementException noSuchElementException) {
      throw new TestAbortedException(
          "missing test configuration, please set token and repository in system properties / environment variables",
          noSuchElementException);
    }

    this.service = configSource.service;
    this.configRegistry =
        ConfigRegistry.create(
            ConfigRegistrySettings.builder()
                .addLastSource("ScalecubeConfigurationService", configSource)
                .addFirstSource("System", new SystemPropertiesConfigSource())
                .addLastSource("ENV", new SystemEnvironmentConfigSource())
                .reloadIntervalSec(1)
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
