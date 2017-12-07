package io.scalecube.config.keyvalue;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigSourceNotAvailableException;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.LoadedConfigProperty;
import io.scalecube.config.utils.ThrowableUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class KeyValueConfigSource implements ConfigSource {
  private static final Logger LOGGER = LoggerFactory.getLogger(KeyValueConfigSource.class);

  private static final ThreadFactory threadFactory;
  static {
    threadFactory = r -> {
      Thread thread = new Thread(r);
      thread.setDaemon(true);
      thread.setName("keyvalue-config-executor");
      thread.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Exception occurred: " + e, e));
      return thread;
    };
  }

  private static final Executor executor = Executors.newCachedThreadPool(threadFactory);

  private final KeyValueConfigRepository repository;
  private final Duration repositoryTimeout;
  private final List<CollectionWithGroup> collections;

  private KeyValueConfigSource(Builder builder) {
    this.repository = builder.repository;
    this.repositoryTimeout = builder.repositoryTimeout;
    this.collections = configureCollections(builder.groupList, builder.collectionName);
  }

  private List<CollectionWithGroup> configureCollections(List<String> groupList, String collectionName) {
    List<String> result = new ArrayList<>();
    result.addAll(groupList);
    result.add(null); // by default 'root' group is always added
    return result.stream().map(input -> new CollectionWithGroup(input, collectionName)).collect(Collectors.toList());
  }

  public static Builder withRepository(KeyValueConfigRepository repository) {
    return new Builder(repository);
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    List<CompletableFuture<List<KeyValueConfigEntity>>> futureList = collections.stream()
        .map(input -> CompletableFuture.supplyAsync(
            () -> repository.findAll(input.getGroupName().orElse(null), input.getCollectionName()), executor))
        .collect(Collectors.toList());

    CompletableFuture<Void> allResults =
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()]));

    CompletableFuture<List<List<KeyValueConfigEntity>>> joinedFuture =
        allResults.thenApply(input -> futureList.stream().map(CompletableFuture::join).collect(Collectors.toList()));

    List<List<KeyValueConfigEntity>> resultList;
    try {
      resultList = joinedFuture.get(repositoryTimeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (ExecutionException e) {
      throw ThrowableUtil.propagate(e.getCause());
    } catch (TimeoutException e) {
      String message = String.format("TimeoutException after '%s' millis", repositoryTimeout.toMillis());
      throw new ConfigSourceNotAvailableException(message, e);
    } catch (InterruptedException e) {
      Thread.interrupted();
      throw ThrowableUtil.propagate(e);
    }

    return resultList.stream()
        .flatMap(Collection::stream)
        .filter(i -> !i.getDisabled())
        .collect(Collector.of(
            (Supplier<TreeMap<String, ConfigProperty>>) TreeMap::new,
            (map, i) -> {
              String origin = i.getGroupName().orElse("root");
              String name = i.getPropName();
              String value = i.getPropValue();
              map.putIfAbsent(name, LoadedConfigProperty.withNameAndValue(name, value).origin(origin).build());
            },
            (map1, map2) -> map1));
  }

  public static class Builder {
    private static final Duration DEFAULT_REPOSITORY_TIMEOUT = Duration.ofSeconds(3);

    private final KeyValueConfigRepository repository;
    private List<String> groupList = new ArrayList<>();
    private Duration repositoryTimeout = DEFAULT_REPOSITORY_TIMEOUT;
    private String collectionName;

    private Builder(KeyValueConfigRepository repository) {
      this.repository = repository;
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

    public Builder collectionName(String collectionName) {
      this.collectionName = collectionName;
      return this;
    }

    public KeyValueConfigSource build() {
      return new KeyValueConfigSource(this);
    }
  }

  private static class CollectionWithGroup {
    private final String groupName;
    private final String collectionName;

    public CollectionWithGroup(@Nullable String groupName, @Nonnull String collectionName) {
      this.groupName = groupName;
      this.collectionName = Objects.requireNonNull(collectionName);
    }

    public Optional<String> getGroupName() {
      return Optional.ofNullable(groupName);
    }

    public String getCollectionName() {
      return collectionName;
    }
  }
}
