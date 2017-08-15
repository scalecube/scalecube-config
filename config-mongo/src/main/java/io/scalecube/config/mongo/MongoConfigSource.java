package io.scalecube.config.mongo;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.LoadedConfigProperty;
import io.scalecube.config.utils.ConfigCollectorUtil;
import io.scalecube.config.utils.ThrowableUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

public class MongoConfigSource implements ConfigSource {
  private static final String DEFAULT_COLLECTION_NAME = "ConfigurationSource";
  private static final int DEFAULT_MONGO_TIMEOUT_SEC = 3;

  private final MongoConfigConnector connector; // used in toString
  private final MongoConfigRepository repository;
  private final List<String> groupList; // used in toString
  private final List<Predicate<String>> predicates;
  private final int mongoTimeout;

  private MongoConfigSource(Builder builder) {
    this.connector = builder.connector;
    this.mongoTimeout = builder.mongoTimeout;
    this.groupList = configureGroupList(Objects.requireNonNull(builder.groupList));
    this.predicates = this.groupList.stream().map(this::getExactMatchPredicate).collect(Collectors.toList());
    this.repository = new MongoConfigRepository(connector, Objects.requireNonNull(builder.collectionName));
  }

  public static Builder withConnector(@Nonnull MongoConfigConnector connector) {
    return new Builder(Objects.requireNonNull(connector));
  }

  private List<String> configureGroupList(List<String> groupList) {
    Set<String> result = new LinkedHashSet<>();
    result.addAll(groupList);
    result.add(""); // by default 'root' group is always added
    return result.stream().collect(Collectors.toList());
  }

  private Predicate<String> getExactMatchPredicate(String group) {
    return group::equalsIgnoreCase;
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    CompletableFuture<Collection<ConfigurationEntity>> future = repository.findAllAsync(ConfigurationEntity.class);

    Stream<ConfigurationEntity> configEntityStream;
    try {
      configEntityStream = future.get(mongoTimeout, TimeUnit.SECONDS).stream();
    } catch (Exception e) {
      throw ThrowableUtil.propagate(e);
    }

    Function<Set<ConfigurationEntity>, String> getFirstPropValue = set -> set.iterator().next().getPropValue();

    Map<String, Map<String, String>> configMap = configEntityStream
        .filter(entity -> entity.getEnabled() == null || entity.getEnabled())
        .collect(Collectors.groupingBy(entity -> Optional.ofNullable(entity.getGroupName()).orElse(""),
            Collectors.groupingBy(ConfigurationEntity::getPropName,
                Collectors.collectingAndThen(Collectors.toCollection(HashSet::new), getFirstPropValue))));

    Map<String, ConfigProperty> result = new TreeMap<>();
    ConfigCollectorUtil.filterAndCollectInOrder(predicates.iterator(), configMap,
        (group, map) -> map.entrySet().forEach(
            entry -> result.putIfAbsent(entry.getKey(),
                LoadedConfigProperty.withNameAndValue(entry).origin(group).build())));

    return result;
  }

  @Override
  public String toString() {
    Map<Integer, String> orderedGroups = new TreeMap<>();
    int order = 0;
    for (String group : this.groupList) {
      orderedGroups.put(order++, group);
    }
    return "MongoConfigSource{groups=" + orderedGroups + ", connector=" + connector.toString() + '}';
  }

  public static class Builder {
    private final MongoConfigConnector connector;
    private List<String> groupList = new ArrayList<>();
    private int mongoTimeout = DEFAULT_MONGO_TIMEOUT_SEC;
    private String collectionName = DEFAULT_COLLECTION_NAME;

    private Builder(MongoConfigConnector connector) {
      this.connector = connector;
    }

    public Builder groups(String... groups) {
      this.groupList = Arrays.asList(groups);
      return this;
    }

    public Builder groupList(List<String> groupList) {
      this.groupList = groupList;
      return this;
    }

    public Builder mongoTimeout(int mongoTimeout) {
      this.mongoTimeout = mongoTimeout;
      return this;
    }

    public Builder collectionName(String collectionName) {
      this.collectionName = collectionName;
      return this;
    }

    public MongoConfigSource build() {
      return new MongoConfigSource(this);
    }
  }
}
