package io.scalecube.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.scalecube.config.source.ClassPathConfigSource;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ObjectConfigPropertyTest {

  public static final String OBJECT_PROPS_FILE = "ObjectConfigPropertyTest.properties";

  private ConfigRegistryImpl configRegistry;

  @Before
  public void setup() {
    configRegistry = new ConfigRegistryImpl(ConfigRegistrySettings.builder()
        .jmxEnabled(false)
        .addLastSource("source", new ClassPathConfigSource(path -> path.toString().endsWith(OBJECT_PROPS_FILE)))
        .build());
    configRegistry.init();
  }

  @Test
  public void testObjectProperty() throws Exception {
    TestConfig config = configRegistry.objectProperty(TestConfig.class).value(null);

    assertNotNull(config);
    assertEquals(42, config.maxCount);
    assertEquals(Duration.ofMillis(100), config.timeout);
    assertTrue(config.isEnabled);
  }

  @Test
  public void testObjectPropertyNotDefinedInConfigSource() throws Exception {
    Optional<NotDefinedObjectPropertyConfig> configOptional =
        configRegistry.objectProperty(NotDefinedObjectPropertyConfig.class).value();

    assertTrue(configOptional.isPresent());
    assertEquals(42, configOptional.get().iii); // value not changed since nothing resets it
  }

  @Test
  public void testFailedValueParsingOnObjectProperty() throws Exception {
    Optional<IncorrectIntegerValueConfig> configOptional =
        configRegistry.objectProperty(IncorrectIntegerValueConfig.class).value();

    assertFalse(configOptional.isPresent());
  }

  @Test
  public void testPartiallyDefinedValueConfig() throws Exception {
    PartiallyDefinedValueConfig config =
        configRegistry.objectProperty(PartiallyDefinedValueConfig.class).value().get();

    assertEquals(1e7, config.d1, 0); // default value not reset
    assertEquals(42, config.d2, 0);
  }

  @Test
  public void testCustomBindingObjectPropertyConfig() throws Exception {
    Map<String, String> bindingMap = new HashMap<>(); // field name -to- config proeprty name
    bindingMap.put("longList", "test.config.list_of_long_values");
    CustomBindingConfig config =
        configRegistry.objectProperty(bindingMap, CustomBindingConfig.class).value().get();

    assertEquals(Stream.of(1L, 2L, 3L).collect(Collectors.toList()), config.longList);
  }

  @Test
  public void testSkipStaticOrFinalFieldInObjectPropertryClass() throws Exception {
    ConfigClassWithStaticOrFinalField config =
        configRegistry.objectProperty(ConfigClassWithStaticOrFinalField.class).value().get();

    assertEquals(42, config.anInt);
    // fields with modifier 'final' are not taken into account, even if defined in config source
    assertEquals(1, config.finalInt);
  }

  public static class TestConfig {
    private int maxCount;
    private Duration timeout;
    private boolean isEnabled;
  }

  public static class IncorrectIntegerValueConfig {
    private int incorrectInt;
    private String str;
  }

  public static class PartiallyDefinedValueConfig {
    private double d1 = 1e7;
    private double d2 = 1e7;
  }

  public static class CustomBindingConfig {
    private List<Long> longList;
  }

  public static class NotDefinedObjectPropertyConfig {
    private int iii = 42;
  }

  public static class ConfigClassWithStaticOrFinalField {
    static final Logger LOGGER = LoggerFactory.getLogger("logger");
    static final ConfigClassWithStaticOrFinalField defaultInstance = new ConfigClassWithStaticOrFinalField();

    private int anInt = 1;
    private final int finalInt = 1;
  }
}
