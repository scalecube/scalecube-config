package io.scalecube.config.source;

import io.scalecube.config.utils.ThrowableUtil;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class FilteredPathConfigSource implements ConfigSource {
  protected final List<Predicate<Path>> predicates;

  protected FilteredPathConfigSource(List<Predicate<Path>> predicates) {
    Objects.requireNonNull(predicates, "FilteredPathConfigSource: predicates are required");
    this.predicates = Collections.unmodifiableList(predicates);
  }

  protected final Map<Path, Map<String, String>> loadConfigMap(Collection<Path> pathCollection) {
    return pathCollection.stream()
        .filter(path -> predicates.stream().anyMatch(predicate -> predicate.test(path)))
        .collect(Collectors.toMap(path -> path, FilteredPathConfigSource::loadProperties));
  }

  static List<Predicate<Path>> preparePatternPredicates(String mask, List<String> prefixes) {
    Pattern pattern = Pattern.compile(mask);
    Predicate<Path> patternPredicate = path -> pattern.matcher(path.toString()).matches();

    Stream<Predicate<Path>> stream =
        prefixes.stream()
            .map(p -> (Predicate<Path>) path -> path.getFileName().toString().startsWith(p))
            .map(p -> p.and(patternPredicate));

    return Stream.concat(stream, Stream.of(patternPredicate)).collect(Collectors.toList());
  }

  static void filterAndCollectInOrder(
      Iterator<Predicate<Path>> predicateIterator,
      Map<Path, Map<String, String>> configMap,
      BiConsumer<Path, Map<String, String>> configCollector) {

    if (!predicateIterator.hasNext()) {
      return;
    }

    Predicate<Path> groupPredicate = predicateIterator.next();
    List<Path> groups =
        configMap.keySet().stream().filter(groupPredicate).collect(Collectors.toList());
    for (Path group : groups) {
      Map<String, String> map = configMap.get(group);
      if (!map.isEmpty()) {
        configCollector.accept(group, map);
      }
    }

    filterAndCollectInOrder(predicateIterator, configMap, configCollector);
  }

  private static Map<String, String> loadProperties(Path input) {
    try (InputStream is = input.toUri().toURL().openStream()) {
      Properties properties = new Properties();
      properties.load(is);
      return fromProperties(properties);
    } catch (Exception e) {
      throw ThrowableUtil.propagate(e);
    }
  }

  private static Map<String, String> fromProperties(Properties properties) {
    Map<String, String> map = new HashMap<>();
    for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements(); ) {
      String key = (String) e.nextElement();
      map.put(key, properties.getProperty(key));
    }
    return map;
  }
}
