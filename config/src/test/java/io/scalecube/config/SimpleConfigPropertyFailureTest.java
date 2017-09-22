package io.scalecube.config;

import static io.scalecube.config.TestUtil.WAIT_FOR_RELOAD_PERIOD_MILLIS;
import static io.scalecube.config.TestUtil.mapBuilder;
import static io.scalecube.config.TestUtil.newConfigRegistry;
import static io.scalecube.config.TestUtil.toConfigProps;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.scalecube.config.source.ConfigSource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class SimpleConfigPropertyFailureTest {
  @Mock
  ConfigSource configSource;
  @Mock
  SideEffect sideEffect;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testValueAbsentAndValidationNotPassed() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Validation failed");

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    BooleanConfigProperty booleanProperty = configRegistry.booleanProperty("bool");
    booleanProperty.addValidator(Objects::nonNull); // proper, 'must have', validation check
  }

  @Test
  public void testValueRemovedOnReloadValidationNotPassed() throws Exception {
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
  public void testFailingValueParser() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Exception occured at valueParser");

    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder().put("int", "not an int").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    configRegistry.intProperty("int");
  }

  @Test
  public void testValueParserFailingOnReload() throws Exception {
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
  public void testValidationNotPassed() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Validation failed");

    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder().put("prop", "1").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    IntConfigProperty intProperty = configRegistry.intProperty("prop");
    intProperty.addValidator(i -> i >= 42);
  }

  @Test
  public void testValidationFailingWithNullPointerIfNullValueApplied() throws Exception {
    thrown.expect(NullPointerException.class);

    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder().put("prop", "1").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    IntConfigProperty intProperty = configRegistry.intProperty("prop_not_found");
    intProperty.addValidator(i -> i >= 42); // NOTE there's no check for not-null
  }

  public interface SideEffect {

    boolean apply(Object t1, Object t2);
  }
}
