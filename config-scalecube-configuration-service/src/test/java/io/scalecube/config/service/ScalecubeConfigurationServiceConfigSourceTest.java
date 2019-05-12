package io.scalecube.config.service;

import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.POJONode;
import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ObjectConfigProperty;
import io.scalecube.configuration.api.ConfigurationService;
import io.scalecube.configuration.api.SaveRequest;
import io.scalecube.test.fixtures.Fixtures;
import io.scalecube.test.fixtures.WithFixture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;

@WithFixture(LocalMockServiceFixture.class)
@ExtendWith(Fixtures.class)
class ScalecubeConfigurationServiceConfigSourceTest {

  @ParameterizedTest
  void test(ConfigRegistry sut, ConfigurationService service) throws InterruptedException {

    CountDownLatch latch = new CountDownLatch(1);
    ObjectConfigProperty<BrokerData> configProperty =
        sut.jsonDocumentProperty("MyKey1", BrokerData.class);

    configProperty.addCallback(
        (old, newv) -> {
          System.out.println(newv);
          latch.countDown();
        });
    BrokerData expectedObject = new BrokerData();
    expectedObject.BrokerID = "asdfasdfasdf";
    expectedObject.APIKeys = new ArrayList<>();
    ApiKey key = new ApiKey();
    key.APIKey = "ASDF";
    key.Permissions = Collections.emptyList();
    expectedObject.APIKeys.add(key);
    JsonNode value = new POJONode(expectedObject);
    service
        .save(
            new SaveRequest(
                sut.stringValue("token", "token"),
                sut.stringValue("repository", "repository"),
                "MyKey1",
                value))
        .block();
    if (!latch.await(5, TimeUnit.SECONDS)) {
      fail();
    }
    Assertions.assertTrue(configProperty.value().isPresent());
  }
}
