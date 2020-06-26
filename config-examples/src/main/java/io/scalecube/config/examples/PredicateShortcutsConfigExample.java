package io.scalecube.config.examples;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.StringConfigProperty;
import io.scalecube.config.source.ClassPathConfigSource;
import io.scalecube.config.source.SystemPropertiesConfigSource;
import java.util.Arrays;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class PredicateShortcutsConfigExample {

  /**
   * Main method for example of predicate ordering.
   *
   * @param args program arguments
   */
  public static void main(String[] args) {
    // Emulate scenario where sys.foo was also given from system properties
    // System.setProperty("sys.foo", "sys foo from java system properties");

    String filename = "config.props";

    ConfigRegistry configRegistry =
        ConfigRegistry.create(
            ConfigRegistrySettings.builder()
                .addLastSource("system", new SystemPropertiesConfigSource())
                .addLastSource(
                    "system.from.file",
                    new SystemPropertiesConfigSource(
                        ClassPathConfigSource.createWithPattern(
                            filename, Arrays.asList("system.override", "system"))))
                .addLastSource(
                    "classpath",
                    ClassPathConfigSource.createWithPattern(
                        filename, Arrays.asList("order.override", "order")))
                .build());

    StringConfigProperty orderedProp1 = configRegistry.stringProperty("orderedProp1");
    String foo = configRegistry.stringProperty("foo").valueOrThrow();
    String bar = configRegistry.stringProperty("bar").valueOrThrow();
    String baz = configRegistry.stringProperty("baz").valueOrThrow();
    String sysFoo = configRegistry.stringProperty("sys.foo").valueOrThrow();

    System.out.println(
        "### Matched by first predicate: orderedProp1=" + orderedProp1.value().get());
    System.out.println("### By predicates: foo=" + foo + ", bar=" + bar + ", baz=" + baz);
    System.out.println(
        "### Custom system property: sysFoo="
            + sysFoo
            + ", System.getProperty(sysFoo)="
            + System.getProperty("sys.foo"));
  }
}
