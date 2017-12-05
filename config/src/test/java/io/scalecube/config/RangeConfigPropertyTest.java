package io.scalecube.config;

import io.scalecube.config.RangeConfigProperty.Range;
import io.scalecube.config.source.ConfigSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;
import java.util.Map;

import static io.scalecube.config.TestUtil.mapBuilder;
import static io.scalecube.config.TestUtil.newConfigRegistry;
import static io.scalecube.config.TestUtil.toConfigProps;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RangeConfigPropertyTest {
  @Mock
  ConfigSource configSource;

  @Test
  public void testRangeProperty() {
    Range<Integer> expectedIntRange = new Range<>(1, 12);
    Range<Duration> expectedDurationRange = new Range<>(Duration.ofSeconds(2), Duration.ofSeconds(12));
    when(configSource.loadConfig())
      .thenReturn(toConfigProps(mapBuilder()
        .put("int_range", "1..12")
        .put("int_range_with_spaces", " 1 ..  12  ")
        .put("long_range", "1..12")
        .put("double_range", "1.9 .. 11.99")
        .put("duration_range", "2s .. 12s")
        .build()));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    assertEquals(expectedIntRange, configRegistry.intRangeProperty("int_range").valueOrThrow());
    assertEquals(expectedIntRange, configRegistry.intRangeProperty("int_range_with_spaces").valueOrThrow());
    assertEquals(new Range<>(1L, 12L), configRegistry.longRangeProperty("long_range").valueOrThrow());
    assertEquals(new Range<>(1.9, 11.99), configRegistry.doubleRangeProperty("double_range").valueOrThrow());
    assertEquals(expectedDurationRange, configRegistry.durationRangeProperty("duration_range").valueOrThrow());
  }

  @Test
  public void testWrongRangeProperty() {
    Map<String, String> props = mapBuilder()
      .put("int_range.without_limits", "..")
      .put("int_range.without_right_limit", "1..")
      .put("int_range.without_left_limit", "..2")
      .put("int_range.with_wrong_symbol", "1a..2")
      .put("int_range.without_separator", "12")
      .put("int_range.with_wrong_separator", "1,,2")
      .build();
    when(configSource.loadConfig())
      .thenReturn(toConfigProps(props));

    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    for (String property : props.keySet()) {
      try {
        configRegistry.intRangeProperty(property);
        fail();
      } catch (IllegalArgumentException ignored) {
      }
    }
  }
}
