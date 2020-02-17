package io.scalecube.config.examples;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.StringConfigProperty;
import io.scalecube.config.source.ClassPathConfigSource;
import io.scalecube.config.source.SystemPropertiesConfigSource;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    String mask = ".*config\\.props";

    ConfigRegistry configRegistry =
        ConfigRegistry.create(
            ConfigRegistrySettings.builder()
                .addLastSource("sysProps", new SystemPropertiesConfigSource())
                .addLastSource(
                    "customSysProps",
                    new SystemPropertiesConfigSource(
                        ClassPathConfigSource.createWithPattern(
                            mask, Stream.of("customSys").collect(Collectors.toList()))))
                .addLastSource(
                    "classpath",
                    ClassPathConfigSource.createWithPattern(
                        mask, Stream.of("order1", "order2").collect(Collectors.toList())))
                .build());

    StringConfigProperty orderedProp1 = configRegistry.stringProperty("orderedProp1");
    String foo = configRegistry.stringProperty("foo").valueOrThrow();
    String bar = configRegistry.stringProperty("bar").valueOrThrow();
    String sysFoo = configRegistry.stringProperty("sys.foo").valueOrThrow();

    System.out.println(
        "### Matched by first predicate: orderedProp1=" + orderedProp1.value().get());
    System.out.println("### Regardeless of predicates: foo=" + foo + ", bar=" + bar);
    System.out.println(
        "### Custom system property: sysFoo="
            + sysFoo
            + ", System.getProperty(sysFoo)="
            + System.getProperty("sys.foo"));
  }
}
