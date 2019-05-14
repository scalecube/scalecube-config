package io.scalecube.config;

import static io.scalecube.config.TestUtil.WAIT_FOR_RELOAD_PERIOD_MILLIS;
import static io.scalecube.config.TestUtil.mapBuilder;
import static io.scalecube.config.TestUtil.newConfigRegistry;
import static io.scalecube.config.TestUtil.toConfigProps;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.scalecube.config.source.ConfigSource;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class JsonDocumentConfigPropertyTest {

  @Mock private ConfigSource configSource;

  @Mock private SideEffect sideEffect;

  private TestInfo testInfo;

  @BeforeEach
  public void setUp(TestInfo testInfo) {
    this.testInfo = testInfo;
  }

  // Normal scenarios

  @Test
  public void testObjectProperty() throws Exception {
    when(configSource.loadConfig())
        .thenAnswer(
            answer ->
                toConfigProps(
                    mapBuilder()
                        .put(
                            "testObjectProperty",
                            "{\"maxCount\":42,\"timeout\":\"PT0.1S\",\"isEnabled\":\"true\"}")
                        .build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<TestConfig> configClass = TestConfig.class;
    ObjectConfigProperty<TestConfig> objectProperty =
        configRegistry.jsonObjectProperty("testObjectProperty", configClass);

    TestConfig config = objectProperty.value(null);
    assertNotNull(config);
    assertEquals(42, config.maxCount);
    assertEquals(Duration.ofMillis(100), config.timeout);
    assertTrue(config.isEnabled);
  }

  @Test
  public void testObjectPropertyValidationPassed() {

    String documentKey = "testObjectPropertyValidationPassed";
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(
                mapBuilder()
                    .put(
                        documentKey,
                        "{\"maxCount\":100,\"timeout\":\"PT0.100S\",\"isEnabled\":true}")
                    .build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<TestConfig> configClass = TestConfig.class;
    ObjectConfigProperty<TestConfig> objectProperty =
        configRegistry.jsonObjectProperty(documentKey, configClass);

    objectProperty.addValidator(
        input ->
            input.isEnabled
                && input.maxCount >= 1
                && input.timeout.compareTo(Duration.ofMillis(100)) == 0);

    TestConfig config = objectProperty.value(null);

    assertNotNull(config);
    assertEquals(100, config.maxCount);
    assertEquals(Duration.ofMillis(100), config.timeout);
    assertTrue(config.isEnabled);
  }

  @Test
  public void testReloadObjectPropertyValidationPassed() throws Exception {
    String documentKey = testInfo.getTestMethod().get().getName();
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(
                mapBuilder()
                    .put(documentKey, "{\"maxCount\":1,\"timeout\":\"PT1s\",\"isEnabled\":true}")
                    .build()))
        .thenReturn(
            toConfigProps(
                mapBuilder()
                    .put(documentKey, "{\"maxCount\":42,\"timeout\":\"PT1s\",\"isEnabled\":true}")
                    .build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<TestConfig> configClass = TestConfig.class;
    ObjectConfigProperty<TestConfig> objectProperty =
        configRegistry.jsonObjectProperty(documentKey, configClass);

    objectProperty.addValidator(input -> input.isEnabled && input.maxCount >= 1);
    objectProperty.addCallback((o1, o2) -> sideEffect.apply(o1, o2));

    TestConfig config = objectProperty.value(null);

    assertNotNull(config);
    assertEquals(1, config.maxCount);
    assertEquals(Duration.ofSeconds(1), config.timeout);
    assertTrue(config.isEnabled);

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    TestConfig config1 = objectProperty.value(null);
    assertEquals(42, config1.maxCount);
    assertEquals(Duration.ofSeconds(1), config1.timeout); // note value retained after reload
    assertTrue(config1.isEnabled); // note value retained after reload
    verify(sideEffect).apply(config, config1);
  }

  @Test
  public void testCallbacksNotAppliedOnReloadWhenNothingChanged() throws Exception {
    String documentKey = "testCallbacksNotAppliedOnReloadWhenNothingChanged";
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(mapBuilder().put(documentKey, "{\"anInt\":42,\"aDouble\":42.0}").build()))
        .thenReturn(
            toConfigProps(
                mapBuilder().put(documentKey, "{\"anInt\":42,\"aDouble\":42.0}").build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<SimpleConfig> configClass = SimpleConfig.class;
    ObjectConfigProperty<SimpleConfig> objectProperty =
        configRegistry.jsonObjectProperty(documentKey, configClass);
    objectProperty.addCallback((cfg1, cfg2) -> sideEffect.apply(cfg1, cfg2));

    SimpleConfig config = objectProperty.value(null);
    assertNotNull(config);
    assertEquals(42, config.anInt);
    assertEquals(42.0, config.aDouble);

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    SimpleConfig config1 = objectProperty.value(null);
    assertEquals(42, config1.anInt);
    assertEquals(42.0, config1.aDouble);
    verify(sideEffect, never()).apply(any(), any());
  }

  @Test
  public void testObjectValuesRemovedOnReloadAndNoValidationDefined() throws Exception {
    String documentName = "testObjectValuesRemovedOnReloadAndNoValidationDefined";
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(
                mapBuilder()
                    .put(documentName, "{\"isEnabled1\":\"true\",\"isEnabled2\":true}")
                    .build()))
        .thenAnswer(a -> toConfigProps(mapBuilder().put(documentName, "{}").build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<ConfigValueSoonWillDisappear> configClass = ConfigValueSoonWillDisappear.class;
    ObjectConfigProperty<ConfigValueSoonWillDisappear> objectProperty =
        configRegistry.jsonObjectProperty(documentName, configClass);
    objectProperty.addCallback((cfg1, cfg2) -> sideEffect.apply(cfg1, cfg2));

    ConfigValueSoonWillDisappear config = objectProperty.value(null);

    assertNotNull(config);
    assertTrue(config.isEnabled1);
    assertTrue(config.isEnabled2);

    Throwable t = new TimeoutException();
    CountDownLatch latch = new CountDownLatch(1);
    objectProperty.addCallback(
        (oldVal, newVal) -> {
          try {
            assertFalse(newVal.isEnabled1);
            assertFalse(newVal.isEnabled2);
          } catch (AssertionError collected) {
            t.addSuppressed(collected);
          } finally {
            latch.countDown();
          }
        });
    if (!latch.await(WAIT_FOR_RELOAD_PERIOD_MILLIS + 10, TimeUnit.MILLISECONDS)
        || t.getSuppressed().length > 0) {
      Assertions.fail(t);
    }
    //    verify(sideEffect).apply(config, null);
  }

  @Test
  public void testObjectValuesAddedOnReloadAndNoValidationDefined() throws Exception {
    String documentKey = testInfo.getTestMethod().get().getName();
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(mapBuilder().build())) // note that no config props defined at start
        .thenReturn(toConfigProps(mapBuilder().put(documentKey, "{\"i\":1,\"j\":1}").build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<ConfigValueWillBeAdded> configClass = ConfigValueWillBeAdded.class;
    ObjectConfigProperty<ConfigValueWillBeAdded> objectProperty =
        configRegistry.jsonObjectProperty(documentKey, configClass);
    objectProperty.addCallback((cfg1, cfg2) -> sideEffect.apply(cfg1, cfg2));

    assertFalse(objectProperty.value().isPresent());
    assertNull(objectProperty.value(null));

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertTrue(objectProperty.value().isPresent());
    ConfigValueWillBeAdded config = objectProperty.value(null);
    assertNotNull(config);
    assertEquals(1, config.i);
    assertEquals(1, config.j);
    verify(sideEffect).apply(null, config);
  }

  @Test
  public void testObjectPropertyNotDefinedInConfigSource() {
    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder().build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);
    Class<NotDefinedObjectPropertyConfig> configClass = NotDefinedObjectPropertyConfig.class;
    ObjectConfigProperty<NotDefinedObjectPropertyConfig> objectProperty =
        configRegistry.jsonObjectProperty(configClass.getName(), configClass);

    assertFalse(objectProperty.value().isPresent());
    assertNull(objectProperty.value(null));
  }

  @Test
  public void testPartiallyDefinedValueConfig() {
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(
                mapBuilder().put("testPartiallyDefinedValueConfig", "{\"d2\":42}").build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<PartiallyDefinedValueConfig> configClass = PartiallyDefinedValueConfig.class;
    PartiallyDefinedValueConfig config =
        configRegistry
            .jsonObjectProperty("testPartiallyDefinedValueConfig", configClass)
            .value()
            .get();

    assertEquals(1e7, config.d1); // default value not changed
    assertEquals(42, config.d2); // value came from config
  }

  @Test
  public void testSkipStaticOrFinalFieldInObjectPropertryClass() {
    String documentKey = testInfo.getTestMethod().get().getName();

    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(
                mapBuilder().put(documentKey, "{\"anInt\":42, \"finalInt\":100}").build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<ConfigClassWithStaticOrFinalField> configClass = ConfigClassWithStaticOrFinalField.class;
    ConfigClassWithStaticOrFinalField config =
        configRegistry.jsonObjectProperty(documentKey, configClass).value().get();

    assertEquals(42, config.anInt);
    // fields with modifier 'final' are not taken into account, even if defined in config source
    assertEquals(1, config.finalInt);
  }

  // Failure scenarios

  @Test
  public void testValueAbsentAndValidationNotPassed() {
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    ObjectConfigProperty<ConnectorSettings> objectProperty =
        configRegistry.jsonObjectProperty("connector", ConnectorSettings.class);

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          objectProperty.addValidator(Objects::nonNull);
          objectProperty.addValidator(
              settings -> settings.user != null && settings.password != null);
        },
        "Validation failed");
  }

  @Test
  public void testValueRemovedOnReloadValidationNotPassed() throws Exception {
    String documentKey = "connector";
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(
                mapBuilder().put(documentKey, "{\"user\":\"yada\",\"password\":\"yada\"}").build()))
        .thenReturn(toConfigProps(mapBuilder().build())); // -> prorperties gone
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    ObjectConfigProperty<ConnectorSettings> objectProperty =
        configRegistry.jsonObjectProperty(documentKey, ConnectorSettings.class);
    objectProperty.addValidator(Objects::nonNull);
    objectProperty.addValidator(settings -> settings.user != null && settings.password != null);
    objectProperty.addCallback((i1, i2) -> sideEffect.apply(i1, i2));

    assertEquals("yada", objectProperty.value().get().user);
    assertEquals("yada", objectProperty.value().get().password);

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertTrue(objectProperty.value().isPresent());
    assertEquals("yada", objectProperty.value().get().user);
    assertEquals("yada", objectProperty.value().get().password);
    verify(sideEffect, never()).apply(any(), any());
  }

  @Test
  public void testValueParserFailingOnReload() throws Exception {
    String documentKey = "com.acme";
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().put(documentKey, "{\"anInt\":1}").build()))
        .thenReturn(
            toConfigProps(mapBuilder().put(documentKey, "{\"anInt\":\"not an int\"}").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    ObjectConfigProperty<IntObjectSettings> objectProperty =
        configRegistry.jsonObjectProperty(documentKey, IntObjectSettings.class);
    objectProperty.addValidator(Objects::nonNull);
    objectProperty.addValidator(settings -> settings.anInt >= 1);
    objectProperty.addCallback((i1, i2) -> sideEffect.apply(i1, i2));

    assertEquals(1, objectProperty.value().get().anInt);

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertTrue(objectProperty.value().isPresent());
    assertEquals(1, objectProperty.value().get().anInt);
    verify(sideEffect, times(1)).apply(any(), any());
  }

  @Test
  void testValidationNotPassed() {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().put("com.acme", "{\"anInt\":1}").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    ObjectConfigProperty<IntObjectSettings> objectProperty =
        configRegistry.jsonObjectProperty("com.acme", IntObjectSettings.class);

    assertThrows(
        IllegalArgumentException.class,
        () -> objectProperty.addValidator(settings -> settings.anInt >= 42),
        "Validation failed");
  }

  public static class TestConfig {
    public int maxCount;
    public Duration timeout;
    public boolean isEnabled;
  }

  public static class IncorrectIntegerValueConfig {
    public int incorrectInt;
    public String str;
  }

  public static class PartiallyDefinedValueConfig {
    public double d1 = 1e7;
    public double d2 = 1e7;
  }

  public static class NotDefinedObjectPropertyConfig {
    public int iii = 42;
  }

  public static class ConfigClassWithStaticOrFinalField {
    static final Logger LOGGER = LoggerFactory.getLogger("logger");
    public static final ConfigClassWithStaticOrFinalField defaultInstance =
        new ConfigClassWithStaticOrFinalField();

    public int anInt = 1;
    public final int finalInt = 1;
  }

  public static class SimpleConfig {
    public int anInt = 100;
    public double aDouble = 200.0;
  }

  public static class ConfigValueSoonWillDisappear {
    public boolean isEnabled1;
    public boolean isEnabled2;
  }

  public static class ConfigValueSoonWillDisappearPartially {
    public long x;
    public long primLong;
    public Long objLong;
  }

  public static class ConfigValueWillBeAdded {
    public int i = -1;
    public int j = -1;
  }

  public interface SideEffect {
    boolean apply(Object t1, Object t2);
  }

  public static class ConnectorSettings {
    public String user;
    public String password;
  }

  public static class IntObjectSettings {
    public int anInt;
  }
}
