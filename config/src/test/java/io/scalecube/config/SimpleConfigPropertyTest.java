package io.scalecube.config;

import static io.scalecube.config.TestUtil.WAIT_FOR_RELOAD_PERIOD_MILLIS;
import static io.scalecube.config.TestUtil.mapBuilder;
import static io.scalecube.config.TestUtil.newConfigRegistry;
import static io.scalecube.config.TestUtil.toConfigProps;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.scalecube.config.source.ConfigSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class SimpleConfigPropertyTest {
  @Mock
  ConfigSource configSource;
  @Mock
  SideEffect sideEffect;

  @Test
  public void testValueFoundAndNoValidationDefined() throws Exception {
    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder().put("int", "1").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    IntConfigProperty intProperty = configRegistry.intProperty("int");
    assertTrue(intProperty.value().isPresent());
    assertEquals(1, (int) intProperty.value().get());
  }

  @Test
  public void testValueAbsentAndNoValidationDefined() throws Exception {
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    IntConfigProperty intProperty = configRegistry.intProperty("int");
    assertFalse(intProperty.value().isPresent());
  }

  @Test
  public void testValueFoundAndValidationPassed() throws Exception {
    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder().put("bool", "true").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    BooleanConfigProperty booleanProperty = configRegistry.booleanProperty("bool");
    booleanProperty.addValidator(bool -> bool);
  }

  @Test
  public void testReloadWithValidationPassed() throws Exception {
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
  public void testCallbacksNotAppliedOnReloadWhenNothingChanged() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().put("ddd", "1.e-3").build()))
        .thenReturn(toConfigProps(mapBuilder().put("ddd", "1.e-3").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    DoubleConfigProperty doubleProperty = configRegistry.doubleProperty("ddd");
    doubleProperty.addValidator(d -> d != null && d < 1);
    doubleProperty.addCallback((d1, d2) -> sideEffect.apply(d1, d2));

    assertTrue(doubleProperty.value().isPresent());
    assertEquals(1.e-3, doubleProperty.value().get(), 0);

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertTrue(doubleProperty.value().isPresent());
    assertEquals(1.e-3, doubleProperty.value().get(), 0);
    verify(sideEffect, never()).apply(any(), any());
  }

  @Test
  public void testValueRemovedOnReloadAndNoValidationDefined() throws Exception {
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
  public void testValueShowUpOnReload() throws Exception {
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

  public interface SideEffect {

    boolean apply(Object t1, Object t2);
  }
}
