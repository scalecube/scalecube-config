package io.scalecube.config.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.POJONode;
import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ObjectConfigProperty;
import io.scalecube.configuration.api.ConfigurationService;
import io.scalecube.configuration.api.SaveRequest;
import io.scalecube.test.fixtures.Fixtures;
import io.scalecube.test.fixtures.WithFixture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;

@WithFixture(LocalMockServiceFixture.class)
@WithFixture(ProductionServiceFixture.class)
@ExtendWith(Fixtures.class)
class ScalecubeConfigurationServiceConfigSourceTest {

  @ParameterizedTest
  public void testSingleValue(ConfigRegistry configRegistry, ConfigurationService service)
      throws InterruptedException {

    CountDownLatch latch = new CountDownLatch(1);
    String documentKey = "MyKey1";
    ObjectConfigProperty<BrokerData> configProperty =
        configRegistry.jsonDocumentProperty(documentKey, BrokerData.class);

    configProperty.addCallback(this.onNewValue(latch));

    BrokerData expected =
        new BrokerData(
            "AZSXDC",
            new ApiKey[] {
              new ApiKey(
                  "ASDF", new String[] {"+ALLOW-READ=ALL", "+ALLOW-WRITE=(INSTRUMENT@123345)"}),
              new ApiKey(
                  "QWER", new String[] {"+ALLOW-READ=(INSTRUMENT@123345)", "+ALLOW-WRITE=NONE"})
            });
    JsonNode value = new POJONode(expected);
    service
        .save(
            new SaveRequest(
                configRegistry.stringValue("token", "token"),
                configRegistry.stringValue("repository", "repository"),
                documentKey,
                value))
        .block();
    assertTrue(latch.await(10, TimeUnit.SECONDS), "Time out waiting for a new value");

    assertTrue(configProperty.value().isPresent());
    BrokerData actual = configProperty.value().get();
    assertAll(
        () -> assertEquals(expected.getBrokerID(), actual.getBrokerID(), "Broker ID"),
        () -> assertArrayEquals(expected.getApiKeys(), actual.getApiKeys(), "API keys"),
        () -> assertEquals(expected, actual, "Broker Data"));
  }

  @ParameterizedTest
  public void testChangeValue(ConfigRegistry configRegistry, ConfigurationService service)
      throws InterruptedException {

    CountDownLatch latchForFirst = new CountDownLatch(1);
    CountDownLatch latchForSecond = new CountDownLatch(2);

    String documentKey = "MyKey2";
    ObjectConfigProperty<BrokerData> configProperty =
        configRegistry.jsonDocumentProperty(documentKey, BrokerData.class);

    configProperty.addCallback(this.onNewValue(latchForFirst, latchForSecond));

    BrokerData expected = new BrokerData("IOPHJK", new ApiKey[] {});
    JsonNode value = new POJONode(expected);
    service
        .save(
            new SaveRequest(
                configRegistry.stringValue("token", "token"),
                configRegistry.stringValue("repository", "repository"),
                documentKey,
                value))
        .block();
    assertTrue(latchForFirst.await(10, TimeUnit.SECONDS), "Time out waiting for a new value");
    assertTrue(configProperty.value().isPresent());
    expected = new BrokerData("QAWSED", new ApiKey[] {});
    value = new POJONode(expected);
    service
        .save(
            new SaveRequest(
                configRegistry.stringValue("token", "token"),
                configRegistry.stringValue("repository", "repository"),
                documentKey,
                value))
        .block();
    assertTrue(latchForSecond.await(10, TimeUnit.SECONDS), "Time out waiting for a new value");
    assertTrue(configProperty.value().isPresent());
    BrokerData actual = configProperty.value().get();
    assertEquals(expected, actual, "Broker ID");
  }

  private BiConsumer<BrokerData, BrokerData> onNewValue(CountDownLatch... latches) {
    return (BrokerData oldValue, BrokerData newValue) -> {
      System.out.println(newValue);
      for (CountDownLatch latch : latches) {
        latch.countDown();
      }
    };
  }
}
