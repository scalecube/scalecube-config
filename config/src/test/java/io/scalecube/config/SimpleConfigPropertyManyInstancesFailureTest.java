package io.scalecube.config;

import static io.scalecube.config.TestUtil.WAIT_FOR_RELOAD_PERIOD_MILLIS;
import static io.scalecube.config.TestUtil.mapBuilder;
import static io.scalecube.config.TestUtil.newConfigRegistry;
import static io.scalecube.config.TestUtil.toConfigProps;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
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
public class SimpleConfigPropertyManyInstancesFailureTest {
  @Mock
  ConfigSource configSource;
  @Mock
  SideEffect sideEffect1;
  @Mock
  SideEffect sideEffect2;

  @Test
  public void testValidationNotPassedAtOne() throws Exception {
    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder().put("prop", "1").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    StringConfigProperty stringProperty = configRegistry.stringProperty("prop");
    stringProperty.addValidator(s -> s.length() >= 1);

    try {
      IntConfigProperty intProperty = configRegistry.intProperty("prop");
      intProperty.addValidator(i -> i >= 42);
      fail();
    } catch (Exception e) {
      assertThat(e, is(instanceOf(IllegalArgumentException.class)));
      assertThat(e.getMessage(), containsString("Validation failed"));
    }
  }

  @Test
  public void testReloadValidationNotPassedAtOne() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().put("prop", "42").build()))
        .thenReturn(toConfigProps(mapBuilder().put("prop", "-42").build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    StringConfigProperty stringProperty = configRegistry.stringProperty("prop");
    stringProperty.addValidator(s -> s.length() >= 1);
    stringProperty.addCallback((s1, s2) -> sideEffect1.apply(s1, s2));
    assertTrue(stringProperty.value().isPresent());
    assertEquals("42", stringProperty.value().get());

    IntConfigProperty intProperty = configRegistry.intProperty("prop");
    intProperty.addValidator(i -> i >= 42);
    intProperty.addCallback((i1, i2) -> sideEffect2.apply(i1, i2));
    assertTrue(intProperty.value().isPresent());
    assertEquals(42, (int) intProperty.value().get());

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertTrue(stringProperty.value().isPresent());
    assertEquals("-42", stringProperty.value().get());
    verify(sideEffect1).apply("42", "-42");

    assertTrue(intProperty.value().isPresent());
    assertEquals(42, (int) intProperty.value().get());
    verify(sideEffect2, never()).apply(anyInt(), anyInt());
  }

  public interface SideEffect {

    boolean apply(Object t1, Object t2);
  }
}
