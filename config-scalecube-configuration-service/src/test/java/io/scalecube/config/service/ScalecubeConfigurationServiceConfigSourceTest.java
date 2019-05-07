package io.scalecube.config.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectReader;
import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.ObjectConfigProperty;
import io.scalecube.config.StringConfigProperty;
import io.scalecube.config.utils.ObjectMapperHolder;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.FluxSink;
import reactor.test.StepVerifier;

class ScalecubeConfigurationServiceConfigSourceTest {

  private ConfigRegistry sut;

  @BeforeEach
  void setUp() throws Exception {

    String token = "SECRET";
    String repository = "DudiRepo1";
    sut =
        ConfigRegistry.create(
            ConfigRegistrySettings.builder()
                .addLastSource(
                    "ScalecubeConfigurationService",
                    new ScalecubeConfigurationServiceConfigSource(token, repository))
                .reloadIntervalSec(3)
                .build());
  }

  @AfterEach
  void tearDown() throws Exception {}

  @Test
  void test() throws InterruptedException {

    CountDownLatch latch = new CountDownLatch(1);
    ObjectConfigProperty<BrokerData> configProperty =
        sut.jsonDocumentProperty("DudiKey1", BrokerData.class);

    configProperty.addCallback(
        (old, newv) -> {
          System.out.println(newv);
          latch.countDown();
        });
    Assertions.assertTrue(configProperty.value().isPresent());
    if (!latch.await(5, TimeUnit.SECONDS)) {
      fail();
    }
    Assertions.assertTrue(configProperty.value().isPresent());
  }
}
