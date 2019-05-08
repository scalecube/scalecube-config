package io.scalecube.config.service;

import static org.junit.jupiter.api.Assertions.fail;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.ObjectConfigProperty;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    if (!latch.await(5, TimeUnit.SECONDS)) {
      fail();
    }
    Assertions.assertTrue(configProperty.value().isPresent());
  }
}
