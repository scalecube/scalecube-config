package io.scalecube.config.keyvalue;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigSourceNotAvailableException;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.LoadedConfigProperty;
import io.scalecube.config.utils.ThrowableUtil;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Generic key-value config source. Communicates with concrete config data source (mongodb, redis,
 * zookeeper) using injectable {@link #repository}.
 */
public class KeyValueConfigSource implements ConfigSource {

  private static final Logger LOGGER = System.getLogger(KeyValueConfigSource.class.getName());

  private static final ThreadFactory threadFactory;

  static {
    threadFactory =
        r -> {
          Thread thread = new Thread(r);
          thread.setDaemon(true);
          thread.setName("keyvalue-config-executor");
          thread.setUncaughtExceptionHandler(
              (t, e) -> LOGGER.log(Level.ERROR, "Exception occurred: {0}", e));
          return thread;
        };
  }

  private static final Executor executor = Executors.newCachedThreadPool(threadFactory);

  private final KeyValueConfigRepository repository;
  private final Duration repositoryTimeout;
  private final List<KeyValueConfigName> configNames; // calculated field

  private KeyValueConfigSource(Builder builder) {
    this.repository = builder.repository;
    this.repositoryTimeout = builder.repositoryTimeout;
    this.configNames = configureConfigNames(builder.groupList, builder.collectionName);
  }

  private static List<KeyValueConfigName> configureConfigNames(
      List<String> groupList, String collectionName) {
    List<String> result = new ArrayList<>();
    result.addAll(groupList);
    result.add(null); // by default 'root' group is always added
    return result.stream()
        .map(input -> new KeyValueConfigName(input, collectionName))
        .collect(Collectors.toList());
  }

  public static Builder withRepository(KeyValueConfigRepository repository) {
    return new Builder(repository);
  }

  public static Builder withRepository(KeyValueConfigRepository repository, String collectionName) {
    return new Builder(repository, collectionName);
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    List<CompletableFuture<List<KeyValueConfigEntity>>> futureList =
        configNames.stream().map(this::loadConfig).collect(Collectors.toList());

    CompletableFuture<Void> allResults =
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()]));

    CompletableFuture<List<List<KeyValueConfigEntity>>> joinedFuture =
        allResults.thenApply(
            input -> futureList.stream().map(CompletableFuture::join).collect(Collectors.toList()));

    List<List<KeyValueConfigEntity>> resultList;
    try {
      resultList = joinedFuture.get(repositoryTimeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (ExecutionException e) {
      throw ThrowableUtil.propagate(e.getCause());
    } catch (TimeoutException e) {
      String message =
          String.format("TimeoutException after '%s' millis", repositoryTimeout.toMillis());
      throw new ConfigSourceNotAvailableException(message, e);
    } catch (InterruptedException e) {
      Thread.interrupted();
      throw ThrowableUtil.propagate(e);
    }

    return resultList.stream()
        .flatMap(Collection::stream)
        .filter(i -> !i.getDisabled())
        .collect(
            Collector.of(
                (Supplier<TreeMap<String, ConfigProperty>>) TreeMap::new,
                (map, i) -> {
                  String origin = i.getConfigName().getQualifiedName();
                  String name = i.getPropName();
                  String value = i.getPropValue();
                  map.putIfAbsent(
                      name,
                      LoadedConfigProperty.withNameAndValue(name, value).origin(origin).build());
                },
                (map1, map2) -> map1));
  }

  private CompletableFuture<List<KeyValueConfigEntity>> loadConfig(KeyValueConfigName configName) {
    return CompletableFuture.supplyAsync(
        () -> {
          List<KeyValueConfigEntity> result;
          try {
            result = repository.findAll(configName);
          } catch (Exception e) {
            LOGGER.log(
                Level.WARNING,
                "Exception at {0}.findAll({1}) {2}",
                repository.getClass().getSimpleName(),
                configName,
                e);
            result = Collections.emptyList();
          }
          return result;
        },
        executor);
  }

  public static class Builder {
    private static final Duration DEFAULT_REPOSITORY_TIMEOUT = Duration.ofSeconds(3);
    private static final String DEFAULT_COLLECTION_NAME = "KeyValueConfigSource";

    private final KeyValueConfigRepository repository;
    private final String collectionName;
    private List<String> groupList = new ArrayList<>();
    private Duration repositoryTimeout = DEFAULT_REPOSITORY_TIMEOUT;

    private Builder(KeyValueConfigRepository repository) {
      this(repository, DEFAULT_COLLECTION_NAME);
    }

    private Builder(KeyValueConfigRepository repository, String collectionName) {
      this.repository = Objects.requireNonNull(repository);
      this.collectionName = Objects.requireNonNull(collectionName);
    }

    public Builder groups(String... groups) {
      this.groupList = Arrays.asList(groups);
      return this;
    }

    public Builder groupList(List<String> groupList) {
      this.groupList = groupList;
      return this;
    }

    public Builder repositoryTimeout(Duration repositoryTimeout) {
      this.repositoryTimeout = repositoryTimeout;
      return this;
    }

    public KeyValueConfigSource build() {
      return new KeyValueConfigSource(this);
    }
  }
}
