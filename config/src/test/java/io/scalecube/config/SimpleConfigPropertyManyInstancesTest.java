package io.scalecube.config;

import static io.scalecube.config.TestUtil.WAIT_FOR_RELOAD_PERIOD_MILLIS;
import static io.scalecube.config.TestUtil.mapBuilder;
import static io.scalecube.config.TestUtil.newConfigRegistry;
import static io.scalecube.config.TestUtil.toConfigProps;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.scalecube.config.source.ConfigSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class SimpleConfigPropertyManyInstancesTest {
  @Mock
  ConfigSource configSource;
  @Mock
  SideEffect sideEffect1;
  @Mock
  SideEffect sideEffect2;

  @Test
  public void testManyInstancesValueNullInitially() throws Exception {
    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder().build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    DoubleConfigProperty doubleProperty = configRegistry.doubleProperty("prop");
    assertFalse(doubleProperty.value().isPresent());

    StringConfigProperty stringProperty = configRegistry.stringProperty("prop");
    assertFalse(stringProperty.value().isPresent());
  }

  @Test
  public void testReloadValueBecameNotNull() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().build()))
        .thenReturn(toConfigProps(mapBuilder().put("prop", "1.e-3").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    DoubleConfigProperty doubleProperty = configRegistry.doubleProperty("prop");
    doubleProperty.addCallback((d1, d2) -> sideEffect1.apply(d1, d2));
    assertFalse(doubleProperty.value().isPresent());

    StringConfigProperty stringProperty = configRegistry.stringProperty("prop");
    stringProperty.addCallback((s1, s2) -> sideEffect2.apply(s1, s2));
    assertFalse(stringProperty.value().isPresent());

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertTrue(doubleProperty.value().isPresent());
    verify(sideEffect1).apply(null, 0.001);

    assertTrue(stringProperty.value().isPresent());
    verify(sideEffect2).apply(null, "1.e-3");
  }

  @Test
  public void testManyInstancesNoValidationOnBoths() throws Exception {
    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder().put("prop", "1").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    ListConfigProperty<Integer> intListProperty = configRegistry.intListProperty("prop");
    assertTrue(intListProperty.value().isPresent());
    assertEquals(Collections.singletonList(1), intListProperty.value().get());

    LongConfigProperty longProperty = configRegistry.longProperty("prop");
    assertTrue(longProperty.value().isPresent());
    assertEquals(1, (long) longProperty.value().get());
  }

  @Test
  public void testReloadValidationPassedOnBoths() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().put("prop", "1").build()))
        .thenReturn(toConfigProps(mapBuilder().put("prop", "42").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    StringConfigProperty stringProperty = configRegistry.stringProperty("prop");
    stringProperty.addValidator(s -> s.length() >= 1);
    stringProperty.addCallback((s1, s2) -> sideEffect1.apply(s1, s2));
    assertTrue(stringProperty.value().isPresent());
    assertEquals("1", stringProperty.valueAsString().get());

    IntConfigProperty intProperty = configRegistry.intProperty("prop");
    intProperty.addValidator(i -> i >= 1);
    intProperty.addCallback((i1, i2) -> sideEffect2.apply(i1, i2));
    assertTrue(intProperty.value().isPresent());
    assertEquals(1, (int) intProperty.value().get());

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertTrue(stringProperty.value().isPresent());
    assertEquals("42", stringProperty.valueAsString().get());

    assertTrue(intProperty.value().isPresent());
    assertEquals(42, (int) intProperty.value().get());

    verify(sideEffect1).apply("1", "42");
    verify(sideEffect2).apply(1, 42);
  }

  @Test
  public void testManyInstancesListTypeAndMultimapTypeAndSimplePropertyType() {
    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder().put("prop", "key=value").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    StringConfigProperty stringProperty = configRegistry.stringProperty("prop");
    assertEquals("key=value", stringProperty.valueOrThrow());

    ListConfigProperty<String> stringListProperty = configRegistry.stringListProperty("prop");
    assertEquals(ImmutableList.of("key=value"), stringListProperty.valueOrThrow());

    MultimapConfigProperty<String> stringMultimapProperty = configRegistry.stringMultimapProperty("prop");
    assertEquals(ImmutableMap.of("key", ImmutableList.of("value")), stringMultimapProperty.valueOrThrow());
  }

  public interface SideEffect {

    boolean apply(Object t1, Object t2);
  }
}
