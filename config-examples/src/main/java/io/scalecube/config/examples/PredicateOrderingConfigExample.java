package io.scalecube.config.examples;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.StringConfigProperty;
import io.scalecube.config.source.ClassPathConfigSource;
import io.scalecube.config.source.SystemPropertiesConfigSource;
import java.nio.file.Path;
import java.util.function.Predicate;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class PredicateOrderingConfigExample {

  /**
   * Main method for example of predicate ordering.
   *
   * @param args program arguments
   */
  public static void main(String[] args) {
    Predicate<Path> propsPredicate = path -> path.toString().endsWith(".props");
    Predicate<Path> rootPredicate =
        propsPredicate.and(path -> path.toString().contains("config.props"));
    Predicate<Path> firstPredicate = propsPredicate.and(path -> path.toString().contains("order1"));
    Predicate<Path> secondPredicate =
        propsPredicate.and(path -> path.toString().contains("order2"));
    Predicate<Path> customSysPredicate =
        propsPredicate.and(path -> path.toString().contains("customSys"));

    ConfigRegistry configRegistry =
        ConfigRegistry.create(
            ConfigRegistrySettings.builder()
                .addLastSource(
                    "customSys",
                    new SystemPropertiesConfigSource(new ClassPathConfigSource(customSysPredicate)))
                .addLastSource(
                    "classpath",
                    new ClassPathConfigSource(rootPredicate, firstPredicate, secondPredicate))
                .build());

    StringConfigProperty orderedProp1 = configRegistry.stringProperty("orderedProp1");
    String foo = configRegistry.stringValue("foo", null);
    String bar = configRegistry.stringValue("bar", null);
    String sysFoo = configRegistry.stringValue("sys.foo", null);

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
