package io.scalecube.config.zookeeper.cache;

import io.scalecube.config.keyvalue.KeyValueConfigEntity;
import io.scalecube.config.keyvalue.KeyValueConfigName;
import io.scalecube.config.keyvalue.KeyValueConfigRepository;
import io.scalecube.config.utils.ThrowableUtil;
import io.scalecube.config.zookeeper.ZookeeperConfigConnector;
import io.scalecube.config.zookeeper.ZookeeperSimpleConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ZookeeperScheduledCacheConfigRepository implements KeyValueConfigRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperScheduledCacheConfigRepository.class);

  private static final ScheduledExecutorService reloadExecutor = reloadExecutor();

  private static ScheduledExecutorService reloadExecutor() {
    ThreadFactory threadFactory = r -> {
      Thread thread = new Thread(r);
      thread.setDaemon(true);
      thread.setName("zookeeper-cache-reloader");
      thread.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Exception occurred: " + e, e));
      return thread;
    };
    return Executors.newSingleThreadScheduledExecutor(threadFactory);
  }

  private final ZookeeperSimpleConfigRepository repository;

  private final Map<KeyValueConfigName, List<KeyValueConfigEntity>> props = new ConcurrentHashMap<>();

  public ZookeeperScheduledCacheConfigRepository(@Nonnull ZookeeperConfigConnector connector, @Nonnull Duration delay) {
    this.repository = new ZookeeperSimpleConfigRepository(connector);
    reloadExecutor.scheduleAtFixedRate(this::refresh, 0, delay.toMillis(), TimeUnit.MILLISECONDS);
  }

  @Override
  public List<KeyValueConfigEntity> findAll(@Nonnull KeyValueConfigName configName) {
    return props.computeIfAbsent(configName, k -> fetchAll(configName));
  }

  private void refresh() {
    props.keySet().forEach(configName -> props.put(configName, fetchAll(configName)));
  }

  private List<KeyValueConfigEntity> fetchAll(KeyValueConfigName configName) {
    try {
      return repository.findAll(configName);
    } catch (Exception e) {
      throw ThrowableUtil.propagate(e);
    }
  }
}
