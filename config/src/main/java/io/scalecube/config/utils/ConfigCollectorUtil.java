package io.scalecube.config.utils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ConfigCollectorUtil {

  private ConfigCollectorUtil() {
    // Do not instantiate
  }

  /**
   * Performs filtering and collecting configuration properties.
   *
   * @param predicateIterator iterator over collection of predicates
   * @param configMap configuration map
   * @param configCollector collector function
   * @param <T> type of the input to the predicate
   */
  public static <T> void filterAndCollectInOrder(
      Iterator<Predicate<T>> predicateIterator,
      Map<T, Map<String, String>> configMap,
      BiConsumer<T, Map<String, String>> configCollector) {

    if (!predicateIterator.hasNext()) {
      return;
    }

    Predicate<T> groupPredicate = predicateIterator.next();
    List<T> groups =
        configMap.keySet().stream().filter(groupPredicate).collect(Collectors.toList());
    for (T group : groups) {
      Map<String, String> map = configMap.get(group);
      if (!map.isEmpty()) {
        configCollector.accept(group, map);
      }
    }

    filterAndCollectInOrder(predicateIterator, configMap, configCollector);
  }
}
