package io.scalecube.config.examples;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.DurationConfigProperty;
import io.scalecube.config.ListConfigProperty;
import io.scalecube.config.StringConfigProperty;
import io.scalecube.config.audit.Slf4JConfigEventListener;
import io.scalecube.config.source.DirectoryConfigSource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class ReloadableLocalResourceConfigExample {

  public static void main(String[] args) throws Exception {
    Predicate<Path> reloadablePropsPredicate = path -> path.toString().endsWith(".reloadableProps");
    Predicate<Path> propsPredicate = path -> path.toString().endsWith(".props");

    String basePath = "config-examples/config";

    ConfigRegistry configRegistry =
        ConfigRegistry.create(
            ConfigRegistrySettings
                .builder()
                .addLastSource("configDirectory",
                    new DirectoryConfigSource(basePath, reloadablePropsPredicate, propsPredicate))
                .addListener(new Slf4JConfigEventListener())
                .reloadIntervalSec(1)
                .build());

    StringConfigProperty prop1 = configRegistry.stringProperty("prop1");
    System.out.println("### Initial filesystem config property: prop1=" + prop1.value().get());
    prop1.addCallback((s1, s2) -> System.out
        .println("### Callback called for 'prop1' and value updated from='" + s1 + "' to='" + s2 + "'"));

    File file = createConfigFile(basePath);
    writeValueToProp1(file, "42");
    TimeUnit.SECONDS.sleep(2);
    System.out.println("### Property reloaded: prop1=" + prop1.value().get());

    writeValueToProp1(file, "");
    TimeUnit.SECONDS.sleep(2);
    System.out.println("### Property reloaded again: prop1=" + prop1.value().get());

    file.delete();
    TimeUnit.SECONDS.sleep(2);
    System.out.println("### Property reloaded again and back to its very intial value: prop1=" + prop1.value().get());

    DurationConfigProperty propertyDuration = configRegistry.durationProperty("propertyDuration");
    System.out.println("### Property duration (showing in millis): " + propertyDuration.value().get().toMillis());

    ListConfigProperty<String> propertyList1 = configRegistry.stringListProperty("propertyList1");
    System.out.println("### Property type-list (string): " + propertyList1.value().get());

    ListConfigProperty<Double> propertyList2 = configRegistry.doubleListProperty("propertyList2");
    System.out.println("### Property type-list (double): " + propertyList2.value().get());
  }

  private static void writeValueToProp1(File file, String value) throws IOException {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
      writer.write("prop1=" + value);
      writer.flush();
    }
  }

  private static File createConfigFile(String basePath) {
    File file = new File(basePath + "/config.reloadableProps");
    file.deleteOnExit();
    return file;
  }
}
