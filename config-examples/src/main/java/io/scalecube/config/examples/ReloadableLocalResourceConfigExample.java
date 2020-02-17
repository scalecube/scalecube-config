package io.scalecube.config.examples;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.DurationConfigProperty;
import io.scalecube.config.ListConfigProperty;
import io.scalecube.config.ObjectConfigProperty;
import io.scalecube.config.StringConfigProperty;
import io.scalecube.config.audit.Slf4JConfigEventListener;
import io.scalecube.config.source.FileDirectoryConfigSource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class ReloadableLocalResourceConfigExample {

  /**
   * Main method of example of reloadable config.
   *
   * @param args program arguments
   * @throws Exception exception
   */
  public static void main(String[] args) throws Exception {
    Predicate<Path> reloadablePropsPredicate = path -> path.toString().endsWith(".reloadableProps");
    Predicate<Path> propsPredicate = path -> path.toString().endsWith(".props");

    String basePath = "config-examples/config";

    ConfigRegistry configRegistry =
        ConfigRegistry.create(
            ConfigRegistrySettings.builder()
                .addLastSource(
                    "configDirectory",
                    new FileDirectoryConfigSource(
                        basePath,
                        Stream.of(reloadablePropsPredicate, propsPredicate)
                            .collect(Collectors.toList())))
                .addListener(new Slf4JConfigEventListener())
                .reloadIntervalSec(1)
                .build());

    StringConfigProperty prop1 = configRegistry.stringProperty("prop1");
    System.out.println("### Initial filesystem config property: prop1=" + prop1.value().get());
    prop1.addCallback(
        (s1, s2) ->
            System.out.println(
                "### Callback called for 'prop1' and value updated from='"
                    + s1
                    + "' to='"
                    + s2
                    + "'"));

    ObjectConfigProperty<ObjectConfig> objectProperty =
        configRegistry.objectProperty(
            new HashMap<String, String>() {
              {
                put("anInt", "reloadable.config.test.intProp");
                put("theList", "reloadable.config.test.listProp");
              }
            },
            ObjectConfig.class);
    objectProperty.addCallback(
        (config1, config2) ->
            System.out.println(
                "### Callback called for objectProperty and value updated from='"
                    + config1
                    + "' to='"
                    + config2
                    + "'"));

    File file = createConfigFile(basePath);
    writeProperties(
        file,
        new HashMap<String, String>() {
          {
            put("prop1", "42");
          }
        });
    TimeUnit.SECONDS.sleep(2);
    System.out.println("### Property reloaded: prop1=" + prop1.value().get());

    writeProperties(
        file,
        new HashMap<String, String>() {
          {
            put("prop1", "");
          }
        });
    TimeUnit.SECONDS.sleep(2);
    System.out.println("### Property reloaded again: prop1=" + prop1.value().get());

    writeProperties(
        file,
        new HashMap<String, String>() {
          {
            put("reloadable.config.test.intProp", "1");
            put("reloadable.config.test.listProp", "a,b,c");
          }
        });
    TimeUnit.SECONDS.sleep(2);
    System.out.println("### Object property reloaded: " + objectProperty.value().get());

    file.delete();
    TimeUnit.SECONDS.sleep(2);
    System.out.println(
        "### Property reloaded again and back to its very intial value: prop1="
            + prop1.value().get());

    DurationConfigProperty propertyDuration = configRegistry.durationProperty("propertyDuration");
    System.out.println(
        "### Property duration (showing in millis): " + propertyDuration.value().get().toMillis());

    DurationConfigProperty propertyEnhancedDuration =
        configRegistry.durationProperty("propertyEnhancedDuration");
    System.out.println(
        "### Property enhanced duration (showing in millis): "
            + propertyEnhancedDuration.value().get().toMillis());

    ListConfigProperty<String> propertyList1 = configRegistry.stringListProperty("propertyList1");
    System.out.println("### Property type-list (string): " + propertyList1.value().get());

    ListConfigProperty<Double> propertyList2 = configRegistry.doubleListProperty("propertyList2");
    System.out.println("### Property type-list (double): " + propertyList2.value().get());
  }

  private static void writeProperties(File file, Map<String, String> props) throws IOException {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
      props.forEach(
          (key, value) -> {
            try {
              writer.write(key + "=" + value + "\n");
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
      writer.flush();
    }
  }

  private static File createConfigFile(String basePath) {
    File file = new File(basePath + "/config.reloadableProps");
    file.deleteOnExit();
    return file;
  }

  public static class ObjectConfig {
    private int anInt;
    private List<String> theList;

    public int getAnInt() {
      return anInt;
    }

    public List<String> getTheList() {
      return theList;
    }

    @Override
    public String toString() {
      return "ObjectConfig{" + "anInt=" + anInt + ", theList=" + theList + '}';
    }
  }
}
