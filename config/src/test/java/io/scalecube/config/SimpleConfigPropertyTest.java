package io.scalecube.config;

import static io.scalecube.config.TestUtil.WAIT_FOR_RELOAD_PERIOD_MILLIS;
import static io.scalecube.config.TestUtil.mapBuilder;
import static io.scalecube.config.TestUtil.newConfigRegistry;
import static io.scalecube.config.TestUtil.toConfigProps;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.scalecube.config.source.ConfigSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class SimpleConfigPropertyTest {

  @Mock
  private ConfigSource configSource;
  @Mock
  private SideEffect sideEffect;

  // Normal scenarios

  @Test
  void testValueFoundAndNoValidationDefined() {
    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder().put("int", "1").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    IntConfigProperty intProperty = configRegistry.intProperty("int");
    assertTrue(intProperty.value().isPresent());
    assertEquals(1, (int) intProperty.value().get());
  }

  @Test
  void testValueAbsentAndNoValidationDefined() {
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    IntConfigProperty intProperty = configRegistry.intProperty("int");
    assertFalse(intProperty.value().isPresent());
  }

  @Test
  void testValueFoundAndValidationPassed() {
    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder().put("bool", "true").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    BooleanConfigProperty booleanProperty = configRegistry.booleanProperty("bool");
    booleanProperty.addValidator(bool -> bool);
  }

  @Test
  void testReloadWithValidationPassed() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().put("int", "1").build()))
        .thenReturn(toConfigProps(mapBuilder().put("int", "42").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    IntConfigProperty intProperty = configRegistry.intProperty("int");
    intProperty.addValidator(i -> i >= 1 && i <= 42);
    intProperty.addCallback((i1, i2) -> sideEffect.apply(i1, i2));

    assertTrue(intProperty.value().isPresent());
    assertEquals(1, (int) intProperty.value().get());

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertTrue(intProperty.value().isPresent());
    assertEquals(42, (int) intProperty.value().get());
    verify(sideEffect).apply(1, 42);
  }

  @Test
  void testCallbacksNotAppliedOnReloadWhenNothingChanged() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().put("ddd", "1.e-3").build()))
        .thenReturn(toConfigProps(mapBuilder().put("ddd", "1.e-3").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    DoubleConfigProperty doubleProperty = configRegistry.doubleProperty("ddd");
    doubleProperty.addValidator(d -> d != null && d < 1);
    doubleProperty.addCallback((d1, d2) -> sideEffect.apply(d1, d2));

    assertTrue(doubleProperty.value().isPresent());
    assertEquals(1.e-3, doubleProperty.value().get().doubleValue());

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertTrue(doubleProperty.value().isPresent());
    assertEquals(1.e-3, doubleProperty.value().get().doubleValue());
    verify(sideEffect, never()).apply(any(), any());
  }

  @Test
  void testValueRemovedOnReloadAndNoValidationDefined() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().put("int", "1").build()))
        .thenReturn(toConfigProps(mapBuilder().put("no_more_int", "int property gone").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    IntConfigProperty intProperty = configRegistry.intProperty("int");
    intProperty.addCallback((i1, i2) -> sideEffect.apply(i1, i2));
    assertTrue(intProperty.value().isPresent());
    assertEquals(1, (int) intProperty.value().get());

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertFalse(intProperty.value().isPresent());
    verify(sideEffect).apply(1, null);
  }

  @Test
  void testValueShowUpOnReload() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().put("xyz", "1").build()))
        .thenReturn(toConfigProps(mapBuilder().put("int", "1").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    IntConfigProperty intProperty = configRegistry.intProperty("int");
    intProperty.addCallback((i1, i2) -> sideEffect.apply(i1, i2));
    assertFalse(intProperty.value().isPresent());

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertTrue(intProperty.value().isPresent());
    assertEquals(1, (int) intProperty.value().get());
    verify(sideEffect).apply(null, 1);
  }

  // Failure scenarios

  @Test
  void testValueAbsentAndValidationNotPassed() {
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    BooleanConfigProperty booleanProperty = configRegistry.booleanProperty("bool");

    // proper, 'must have', validation check
    assertThrows(IllegalArgumentException.class,
        () -> booleanProperty.addValidator(Objects::nonNull),
        "Validation failed");
  }

  @Test
  void testValueRemovedOnReloadValidationNotPassed() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().put("int", "1").build()))
        .thenReturn(toConfigProps(mapBuilder().put("no_more_int", "no_more_int").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    IntConfigProperty intProperty = configRegistry.intProperty("int");
    intProperty.addValidator(Objects::nonNull);
    intProperty.addCallback((i1, i2) -> sideEffect.apply(i1, i2));

    assertTrue(intProperty.value().isPresent());
    assertEquals(1, (int) intProperty.value().get());

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertTrue(intProperty.value().isPresent());
    assertEquals(1, (int) intProperty.value().get());
    verify(sideEffect, never()).apply(any(), any());
  }

  @Test
  void testFailingValueParser() {
    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder().put("int", "not an int").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    assertThrows(IllegalArgumentException.class,
        () -> configRegistry.intProperty("int"),
        "Exception occured at valueParser");
  }

  @Test
  void testValueParserFailingOnReload() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().put("int", "1").build()))
        .thenReturn(toConfigProps(mapBuilder().put("int", "not an int").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    IntConfigProperty intProperty = configRegistry.intProperty("int");
    intProperty.addValidator(Objects::nonNull);
    intProperty.addValidator(i -> i >= 1);
    intProperty.addCallback((i1, i2) -> sideEffect.apply(i1, i2));

    assertTrue(intProperty.value().isPresent());
    assertEquals(1, (int) intProperty.value().get());

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertTrue(intProperty.value().isPresent());
    assertEquals(1, (int) intProperty.value().get());
    verify(sideEffect, never()).apply(any(), any());
  }

  @Test
  void testValidationNotPassed() {
    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder().put("prop", "1").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    IntConfigProperty intProperty = configRegistry.intProperty("prop");

    assertThrows(IllegalArgumentException.class,
        () -> intProperty.addValidator(i -> i >= 42),
        "Validation failed");
  }

  @Test
  void testValidationFailingWithNullPointerIfNullValueApplied() {
    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder().put("prop", "1").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    IntConfigProperty intProperty = configRegistry.intProperty("prop_not_found");

    // NOTE there's no check for not-null
    assertThrows(NullPointerException.class, () -> intProperty.addValidator(i -> i >= 42));
  }

  public interface SideEffect {
    boolean apply(Object t1, Object t2);
  }
}
