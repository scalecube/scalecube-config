package io.scalecube.config;

import static io.scalecube.config.TestUtil.WAIT_FOR_RELOAD_PERIOD_MILLIS;
import static io.scalecube.config.TestUtil.mapBuilder;
import static io.scalecube.config.TestUtil.newConfigRegistry;
import static io.scalecube.config.TestUtil.toConfigProps;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.scalecube.config.source.ConfigSource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MultimapConfigPropertyTest {

  @Mock private ConfigSource configSource;

  @Test
  void testIntMultimapProperty() {
    Map<String, List<Integer>> expectedMultimap =
        ImmutableMap.<String, List<Integer>>builder()
            .put("key1", ImmutableList.of(1))
            .put("key2", ImmutableList.of(2, 3, 4))
            .put("key3", ImmutableList.of(5))
            .build();
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(mapBuilder().put("int_multimap", "key1=1,key2=2,3,4,key3=5").build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    assertEquals(
        expectedMultimap, configRegistry.intMultimapProperty("int_multimap").valueOrThrow());
  }

  @Test
  void testLongMultimapProperty() {
    Map<String, List<Long>> expectedMultimap =
        ImmutableMap.<String, List<Long>>builder()
            .put("key1", ImmutableList.of(1L))
            .put("key2", ImmutableList.of(2L, 3L, 4L))
            .put("key3", ImmutableList.of(5L))
            .build();
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(mapBuilder().put("long_multimap", "key1=1,key2=2,3,4,key3=5").build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    assertEquals(
        expectedMultimap, configRegistry.longMultimapProperty("long_multimap").valueOrThrow());
  }

  @Test
  void testDoubleMultimapProperty() {
    Map<String, List<Double>> expectedMultimap =
        ImmutableMap.<String, List<Double>>builder()
            .put("key1", ImmutableList.of(1.1))
            .put("key2", ImmutableList.of(2.2, 3.3, 4.4))
            .put("key3", ImmutableList.of(5.5))
            .build();
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(
                mapBuilder().put("double_multimap", "key1=1.1,key2=2.2,3.3,4.4,key3=5.5").build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    assertEquals(
        expectedMultimap, configRegistry.doubleMultimapProperty("double_multimap").valueOrThrow());
  }

  @Test
  void testDurationMultimapProperty() {
    Map<String, List<Duration>> expectedMultimap =
        ImmutableMap.<String, List<Duration>>builder()
            .put("key1", ImmutableList.of(Duration.ofHours(1)))
            .put(
                "key2",
                ImmutableList.of(
                    Duration.ofMinutes(2), Duration.ofSeconds(3), Duration.ofMillis(4)))
            .put("key3", ImmutableList.of(Duration.ofDays(5)))
            .build();
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(
                mapBuilder().put("duration_multimap", "key1=1h,key2=2m,3s,4ms,key3=5d").build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    assertEquals(
        expectedMultimap,
        configRegistry.durationMultimapProperty("duration_multimap").valueOrThrow());
  }

  @Test
  void testStringMultimapProperty() {
    Map<String, List<String>> expectedMultimap =
        ImmutableMap.<String, List<String>>builder()
            .put("key1", ImmutableList.of("a"))
            .put("key2", ImmutableList.of("b", "c", "d"))
            .put("key3", ImmutableList.of("e"))
            .build();
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(mapBuilder().put("string_multimap", "key1=a,key2=b,c,d,key3=e").build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    assertEquals(
        expectedMultimap, configRegistry.stringMultimapProperty("string_multimap").valueOrThrow());
  }

  @Test
  void testMultimapPropertyWithDuplicatedKeys() {
    Map<String, List<Integer>> expectedMultimap =
        ImmutableMap.<String, List<Integer>>builder()
            .put("key1", ImmutableList.of(1, 6))
            .put("key2", ImmutableList.of(2, 3, 4, 7))
            .put("key3", ImmutableList.of(5))
            .build();
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(
                mapBuilder()
                    .put("int_multimap", "key1=1,key2=2,3,4,key3=5,key1=6,key2=7")
                    .build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    assertEquals(
        expectedMultimap, configRegistry.intMultimapProperty("int_multimap").valueOrThrow());
  }

  @Test
  void testMultimapPropertyWithoutKeyValueSeparator() {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().put("string_multimap", "value1").build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    assertEquals(
        Collections.emptyMap(),
        configRegistry.stringMultimapProperty("string_multimap").valueOrThrow());
  }

  @Test
  void testMultimapPropertyWithoutKey() {
    Map<String, List<String>> expectedMultimap =
        ImmutableMap.<String, List<String>>builder().put("", ImmutableList.of("value1")).build();
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().put("string_multimap", "=value1").build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    assertEquals(
        expectedMultimap, configRegistry.stringMultimapProperty("string_multimap").valueOrThrow());
  }

  @Test
  void testMultimapPropertyWithoutValue() {
    Map<String, List<String>> expectedMultimap =
        ImmutableMap.<String, List<String>>builder().put("key1", ImmutableList.of("")).build();
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().put("string_multimap", "key1=").build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    assertEquals(
        expectedMultimap, configRegistry.stringMultimapProperty("string_multimap").valueOrThrow());
  }

  @Test
  void testMultimapPropertyWithValidatorPassed() {
    Map<String, List<Integer>> expectedMultimap =
        ImmutableMap.<String, List<Integer>>builder()
            .put("key1", ImmutableList.of(1))
            .put("key2", ImmutableList.of(2, 3, 4))
            .put("key3", ImmutableList.of(5))
            .build();
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(mapBuilder().put("int_multimap", "key1=1,key2=2,3,4,key3=5").build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    MultimapConfigProperty<Integer> multimapProperty =
        configRegistry.intMultimapProperty("int_multimap");
    multimapProperty.addValidator(m -> !m.isEmpty());
    assertEquals(expectedMultimap, multimapProperty.valueOrThrow());
  }

  @Test
  void testMultimapPropertyWithValidatorNotPassed() {
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(mapBuilder().put("int_multimap", "key1=1,key2=2,3,4,key3=5").build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    MultimapConfigProperty<Integer> multimapProperty =
        configRegistry.intMultimapProperty("int_multimap");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          multimapProperty.addValidator(Map::isEmpty);
          multimapProperty.valueOrThrow();
        });
  }

  @Test
  void testMultimapPropertyWithCallback() throws InterruptedException {
    AtomicBoolean isCallbackInvoked = new AtomicBoolean(false);
    AtomicBoolean isInvokedInAnotherThread = new AtomicBoolean(false);
    Map<String, List<Integer>> expectedMultimap =
        ImmutableMap.<String, List<Integer>>builder()
            .put("key1", ImmutableList.of(6))
            .put("key2", ImmutableList.of(7, 8, 9))
            .put("key3", ImmutableList.of(10))
            .build();
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(
                mapBuilder().put("int_multimap", "key1=1,key2=2,3,4,key3=5,key4=6").build()))
        .thenReturn(
            toConfigProps(mapBuilder().put("int_multimap", "key1=6,key2=7,8,9,key3=10").build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    MultimapConfigProperty<Integer> multimapProperty =
        configRegistry.intMultimapProperty("int_multimap");
    multimapProperty.addCallback(
        Executors.newSingleThreadExecutor(),
        (m1, m2) -> {
          isCallbackInvoked.set(true);
          isInvokedInAnotherThread.set("config-reloader".equals(Thread.currentThread().getName()));
        });

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertEquals(expectedMultimap, multimapProperty.valueOrThrow());
    assertTrue(isCallbackInvoked.get());
    assertFalse(isInvokedInAnotherThread.get());
  }

  @Test
  void testMultimapPropertyWithCallbackAndExecutor() throws InterruptedException {
    AtomicBoolean isCallbackInvoked = new AtomicBoolean(false);
    AtomicBoolean isInvokedInAnotherThread = new AtomicBoolean(false);
    Map<String, List<Integer>> expectedMultimap =
        ImmutableMap.<String, List<Integer>>builder()
            .put("key1", ImmutableList.of(6))
            .put("key2", ImmutableList.of(7, 8, 9))
            .put("key3", ImmutableList.of(10))
            .build();
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(
                mapBuilder().put("int_multimap", "key1=1,key2=2,3,4,key3=5,key4=6").build()))
        .thenReturn(
            toConfigProps(mapBuilder().put("int_multimap", "key1=6,key2=7,8,9,key3=10").build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    MultimapConfigProperty<Integer> multimapProperty =
        configRegistry.intMultimapProperty("int_multimap");
    multimapProperty.addCallback(
        Executors.newSingleThreadExecutor(),
        (m1, m2) -> {
          isCallbackInvoked.set(true);
          isInvokedInAnotherThread.set(!"config-reloader".equals(Thread.currentThread().getName()));
        });

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertEquals(expectedMultimap, multimapProperty.valueOrThrow());
    assertTrue(isCallbackInvoked.get());
    assertTrue(isInvokedInAnotherThread.get());
  }
}
