package io.scalecube.config;

import static io.scalecube.config.TestUtil.WAIT_FOR_RELOAD_PERIOD_MILLIS;
import static io.scalecube.config.TestUtil.mapBuilder;
import static io.scalecube.config.TestUtil.newConfigRegistry;
import static io.scalecube.config.TestUtil.toConfigProps;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.scalecube.config.source.ConfigSource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(MockitoJUnitRunner.class)
public class ObjectConfigPropertyTest {
  @Mock
  ConfigSource configSource;

  @Mock
  SideEffect sideEffect;

  @Rule
  public TestName testName = new TestName();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  // Normal scenarios

  @Test
  public void testObjectProperty() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder()
            .put("testObjectProperty.maxCount", "42")
            .put("testObjectProperty.timeout", "100ms")
            .put("testObjectProperty.isEnabled", "true")
            .build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<TestConfig> configClass = TestConfig.class;
    ObjectConfigProperty<TestConfig> objectProperty =
        configRegistry.objectProperty(testName.getMethodName(), configClass);

    TestConfig config = objectProperty.value(null);
    assertNotNull(config);
    assertEquals(42, config.maxCount);
    assertEquals(Duration.ofMillis(100), config.timeout);
    assertTrue(config.isEnabled);
  }

  @Test
  public void testObjectPropertyValidationPassed() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder()
            .put("testObjectPropertyValidationPassed.maxCount", "100")
            .put("testObjectPropertyValidationPassed.timeout", "100ms")
            .put("testObjectPropertyValidationPassed.isEnabled", "true")
            .build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<TestConfig> configClass = TestConfig.class;
    ObjectConfigProperty<TestConfig> objectProperty =
        configRegistry.objectProperty(testName.getMethodName(), configClass);

    objectProperty.addValidator(input -> input.isEnabled &&
        input.maxCount >= 1 &&
        input.timeout.compareTo(Duration.ofMillis(100)) == 0);

    TestConfig config = objectProperty.value(null);

    assertNotNull(config);
    assertEquals(100, config.maxCount);
    assertEquals(Duration.ofMillis(100), config.timeout);
    assertTrue(config.isEnabled);
  }

  @Test
  public void testReloadObjectPropertyValidationPassed() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder()
            .put("testReloadObjectPropertyValidationPassed.maxCount", "1") // this value would be changed after reload
            .put("testReloadObjectPropertyValidationPassed.timeout", "1s") // this value would not be changed
            .put("testReloadObjectPropertyValidationPassed.isEnabled", "true") // this value would not be changed
            .build()))
        .thenReturn(toConfigProps(mapBuilder()
            .put("testReloadObjectPropertyValidationPassed.maxCount", "42")
            .put("testReloadObjectPropertyValidationPassed.timeout", "1s")
            .put("testReloadObjectPropertyValidationPassed.isEnabled", "true")
            .build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<TestConfig> configClass = TestConfig.class;
    ObjectConfigProperty<TestConfig> objectProperty =
        configRegistry.objectProperty(testName.getMethodName(), configClass);

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
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder()
            .put("testCallbacksNotAppliedOnReloadWhenNothingChanged.anInt", "42")
            .put("testCallbacksNotAppliedOnReloadWhenNothingChanged.aDouble", "42.0")
            .build()))
        .thenReturn(toConfigProps(mapBuilder()
            .put("testCallbacksNotAppliedOnReloadWhenNothingChanged.anInt", "42")
            .put("testCallbacksNotAppliedOnReloadWhenNothingChanged.aDouble", "42.0")
            .build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<SimpleConfig> configClass = SimpleConfig.class;
    ObjectConfigProperty<SimpleConfig> objectProperty =
        configRegistry.objectProperty(testName.getMethodName(), configClass);
    objectProperty.addCallback((cfg1, cfg2) -> sideEffect.apply(cfg1, cfg2));

    SimpleConfig config = objectProperty.value(null);
    assertNotNull(config);
    assertEquals(42, config.anInt);
    assertEquals(42.0, config.aDouble, 0);

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    SimpleConfig config1 = objectProperty.value(null);
    assertEquals(42, config1.anInt);
    assertEquals(42.0, config1.aDouble, 0);
    verify(sideEffect, never()).apply(any(), any());
  }

  @Test
  public void testObjectValuesRemovedOnReloadAndNoValidationDefined() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder()
            .put("testObjectValuesRemovedOnReloadAndNoValidationDefined.isEnabled1", "true")
            .put("testObjectValuesRemovedOnReloadAndNoValidationDefined.isEnabled2", "true")
            .build()))
        .thenReturn(toConfigProps(mapBuilder()
            .put("testObjectValuesRemovedOnReloadAndNoValidationDefined.noMoreIsEnabledFlags", "all fields gone")
            .build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<ConfigValueSoonWillDisappear> configClass = ConfigValueSoonWillDisappear.class;
    ObjectConfigProperty<ConfigValueSoonWillDisappear> objectProperty =
        configRegistry.objectProperty(testName.getMethodName(), configClass);
    objectProperty.addCallback((cfg1, cfg2) -> sideEffect.apply(cfg1, cfg2));

    ConfigValueSoonWillDisappear config = objectProperty.value(null);
    assertNotNull(config);
    assertTrue(config.isEnabled1);
    assertTrue(config.isEnabled2);

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertFalse(objectProperty.value().isPresent());
    assertNull(objectProperty.value(null)); // value had gone
    verify(sideEffect).apply(config, null);
  }

  @Test
  public void testObjectValueRemovedPartiallyOnReloadAndNoValidationDefined() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder()
            .put("testObjectValueRemovedPartiallyOnReloadAndNoValidationDefined.x", "1")
            .put("testObjectValueRemovedPartiallyOnReloadAndNoValidationDefined.primLong", "2")
            .put("testObjectValueRemovedPartiallyOnReloadAndNoValidationDefined.objLong", "3")
            .build()))
        .thenReturn(toConfigProps(mapBuilder()
            .put("testObjectValueRemovedPartiallyOnReloadAndNoValidationDefined.x", "100500") // note some fields gone
            .build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<ConfigValueSoonWillDisappearPartially> configClass = ConfigValueSoonWillDisappearPartially.class;
    ObjectConfigProperty<ConfigValueSoonWillDisappearPartially> objectProperty =
        configRegistry.objectProperty(testName.getMethodName(), configClass);
    objectProperty.addCallback((cfg1, cfg2) -> sideEffect.apply(cfg1, cfg2));

    ConfigValueSoonWillDisappearPartially config = objectProperty.value(null);
    assertNotNull(config);
    assertEquals(1, config.x);
    assertEquals(2, config.primLong);
    assertEquals((Long) 3L, config.objLong);

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertTrue(objectProperty.value().isPresent());

    ConfigValueSoonWillDisappearPartially config1 = objectProperty.value(null);
    assertNotNull(config1);
    assertEquals(100500, config1.x); // partial config's new value had been set
    assertEquals(0, config1.primLong); // primiteive ==> hence set to default i.e. 0
    assertEquals(null, config1.objLong); // object ==> hence set to default, i.e. null

    verify(sideEffect).apply(config, config1);
  }

  @Test
  public void testObjectValuesAddedOnReloadAndNoValidationDefined() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().build())) // note that no config props defined at start
        .thenReturn(toConfigProps(mapBuilder()
            .put("testObjectValuesAddedOnReloadAndNoValidationDefined.i", "1")
            .put("testObjectValuesAddedOnReloadAndNoValidationDefined.j", "1")
            .build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<ConfigValueWillBeAdded> configClass = ConfigValueWillBeAdded.class;
    ObjectConfigProperty<ConfigValueWillBeAdded> objectProperty =
        configRegistry.objectProperty(testName.getMethodName(), configClass);
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
  public void testObjectPropertyNotDefinedInConfigSource() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);
    Class<NotDefinedObjectPropertyConfig> configClass = NotDefinedObjectPropertyConfig.class;
    ObjectConfigProperty<NotDefinedObjectPropertyConfig> objectProperty =
        configRegistry.objectProperty(configClass.getName(), configClass);

    assertFalse(objectProperty.value().isPresent());
    assertNull(objectProperty.value(null));
  }

  @Test
  public void testFailedValueParsingOnObjectProperty() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("NumberFormatException: For input string: \"int\"");

    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder()
            .put("testFailedValueParsingOnObjectProperty.incorrectInt", "int")
            .put("testFailedValueParsingOnObjectProperty.str=just a string", "int")
            .build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<IncorrectIntegerValueConfig> configClass = IncorrectIntegerValueConfig.class;
    configRegistry.objectProperty(testName.getMethodName(), configClass).value();
  }

  @Test
  public void testPartiallyDefinedValueConfig() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder()
            .put("testPartiallyDefinedValueConfig.d2", "42")
            .build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<PartiallyDefinedValueConfig> configClass = PartiallyDefinedValueConfig.class;
    PartiallyDefinedValueConfig config =
        configRegistry.objectProperty(testName.getMethodName(), configClass).value().get();

    assertEquals(1e7, config.d1, 0); // default value not changed
    assertEquals(42, config.d2, 0); // value came from config
  }

  @Test
  public void testCustomBindingObjectPropertyConfig() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder()
            .put("test.config.list_of_long_values", "1,2,3")
            .build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Map<String, String> bindingMap = new HashMap<>(); // field name -to- config property name
    bindingMap.put("longList", "test.config.list_of_long_values");
    CustomBindingConfig config =
        configRegistry.objectProperty(bindingMap, CustomBindingConfig.class).value().get();

    assertEquals(Stream.of(1L, 2L, 3L).collect(Collectors.toList()), config.longList);
  }

  @Test
  public void testSkipStaticOrFinalFieldInObjectPropertryClass() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder()
            .put("testSkipStaticOrFinalFieldInObjectPropertryClass.anInt", "42")
            .put("testSkipStaticOrFinalFieldInObjectPropertryClass.finalInt", "100")
            .build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    Class<ConfigClassWithStaticOrFinalField> configClass = ConfigClassWithStaticOrFinalField.class;
    ConfigClassWithStaticOrFinalField config =
        configRegistry.objectProperty(testName.getMethodName(), configClass).value().get();

    assertEquals(42, config.anInt);
    // fields with modifier 'final' are not taken into account, even if defined in config source
    assertEquals(1, config.finalInt);
  }

  // Failure scenarios

  @Test
  public void testValueAbsentAndValidationNotPassed() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Validation failed");

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    ObjectConfigProperty<ConnectorSettings> objectProperty =
        configRegistry.objectProperty("connector", ConnectorSettings.class);
    objectProperty.addValidator(Objects::nonNull);
    objectProperty.addValidator(settings -> settings.user != null && settings.password != null);
  }

  @Test
  public void testValueRemovedOnReloadValidationNotPassed() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().put("connector.user", "yada").put("connector.password", "yada").build()))
        .thenReturn(toConfigProps(mapBuilder().build())); // -> prorperties gone
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    ObjectConfigProperty<ConnectorSettings> objectProperty =
        configRegistry.objectProperty("connector", ConnectorSettings.class);
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
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().put("com.acme.anInt", "1").build()))
        .thenReturn(toConfigProps(mapBuilder().put("com.acme.anInt", "not an int").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    ObjectConfigProperty<IntObjectSettings> objectProperty =
        configRegistry.objectProperty("com.acme", IntObjectSettings.class);
    objectProperty.addValidator(Objects::nonNull);
    objectProperty.addValidator(settings -> settings.anInt >= 1);
    objectProperty.addCallback((i1, i2) -> sideEffect.apply(i1, i2));

    assertEquals(1, objectProperty.value().get().anInt);

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertTrue(objectProperty.value().isPresent());
    assertEquals(1, objectProperty.value().get().anInt);
    verify(sideEffect, never()).apply(any(), any());
  }

  @Test
  public void testValidationNotPassed() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Validation failed");

    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder().put("com.acme.anInt", "1").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    ObjectConfigProperty<IntObjectSettings> objectProperty =
        configRegistry.objectProperty("com.acme", IntObjectSettings.class);
    objectProperty.addValidator(settings -> settings.anInt >= 42);
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

  public static class SimpleConfig {
    private int anInt = 100;
    private double aDouble = 200.0;
  }

  public static class ConfigValueSoonWillDisappear {
    private boolean isEnabled1;
    private boolean isEnabled2;
  }

  public static class ConfigValueSoonWillDisappearPartially {
    private long x;
    private long primLong;
    private Long objLong;
  }

  public static class ConfigValueWillBeAdded {
    private int i = -1;
    private int j = -1;
  }

  public interface SideEffect {

    boolean apply(Object t1, Object t2);
  }

  public static class ConnectorSettings {
    String user;
    String password;
  }

  public static class IntObjectSettings {
    int anInt;
  }
}
