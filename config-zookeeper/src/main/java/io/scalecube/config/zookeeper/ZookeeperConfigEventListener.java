package io.scalecube.config.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.scalecube.config.audit.ConfigEvent;
import io.scalecube.config.audit.ConfigEventListener;
import io.scalecube.config.utils.ThrowableUtil;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ZookeeperConfigEventListener implements ConfigEventListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperConfigEventListener.class);

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  private static final ThreadFactory threadFactory;

  static {
    threadFactory = r -> {
      Thread thread = new Thread(r);
      thread.setDaemon(true);
      thread.setName("zookeeper-config-auditor");
      thread.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Exception occurred: " + e, e));
      return thread;
    };
  }

  private static final Executor executor = Executors.newSingleThreadExecutor(threadFactory);

  private final ZookeeperConfigConnector connector;
  private final String collectionPath;

  public ZookeeperConfigEventListener(@Nonnull ZookeeperConfigConnector connector, @Nonnull String collectionPath) {
    this.connector = connector;
    this.collectionPath = "/" + collectionPath;
    create(this.collectionPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
  }

  @Override
  public void onEvent(ConfigEvent event) {
    CompletableFuture.runAsync(() -> {
      AuditLogEvent entity = new AuditLogEvent();
      entity.setName(event.getName());
      entity.setTimestamp(event.getTimestamp());
      entity.setHost(event.getHost());
      entity.setType(event.getType().toString());
      entity.setNewSource(event.getNewSource());
      entity.setNewOrigin(event.getNewOrigin());
      entity.setNewValue(event.getNewValue());
      entity.setOldSource(event.getOldSource());
      entity.setOldOrigin(event.getOldOrigin());
      entity.setOldValue(event.getOldValue());
      insertOne(entity);
    }, executor);
  }

  private void insertOne(AuditLogEvent event) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      ObjectMapper objectMapper = ZookeeperConfigObjectMapper.getInstance();
      objectMapper.writer().writeValue(baos, event);
    } catch (Exception e) {
      LOGGER.error("Exception at converting obj: {} to json, cause: {}", event, e);
      throw ThrowableUtil.propagate(e);
    }
    create(event, baos.toByteArray());
  }

  private void create(AuditLogEvent event, byte[] data) {
    String path = collectionPath + "/" + DATE_FORMAT.format(event.getTimestamp());
    create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    path += "/" + event.getName();
    create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    path += "/event-";
    create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
  }

  private void create(String path, byte[] data, List<ACL> acl, CreateMode mode) {
    try {
      connector.getClient().create(path, data, acl, mode);
    } catch (KeeperException.NodeExistsException e) {
      if (data != null) { // is it end point?
        throw ThrowableUtil.propagate(e);
      }
    } catch (Exception e) {
      throw ThrowableUtil.propagate(e);
    }
  }

  private static class AuditLogEvent {
    private String name;
    private Date timestamp;
    private String type;
    private String host;
    private String oldSource;
    private String oldOrigin;
    private String oldValue;
    private String newSource;
    private String newOrigin;
    private String newValue;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public Date getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(Date timestamp) {
      this.timestamp = timestamp;
    }

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public String getOldSource() {
      return oldSource;
    }

    public void setOldSource(String oldSource) {
      this.oldSource = oldSource;
    }

    public String getOldOrigin() {
      return oldOrigin;
    }

    public void setOldOrigin(String oldOrigin) {
      this.oldOrigin = oldOrigin;
    }

    public String getOldValue() {
      return oldValue;
    }

    public void setOldValue(String oldValue) {
      this.oldValue = oldValue;
    }

    public String getNewSource() {
      return newSource;
    }

    public void setNewSource(String newSource) {
      this.newSource = newSource;
    }

    public String getNewOrigin() {
      return newOrigin;
    }

    public void setNewOrigin(String newOrigin) {
      this.newOrigin = newOrigin;
    }

    public String getNewValue() {
      return newValue;
    }

    public void setNewValue(String newValue) {
      this.newValue = newValue;
    }

    @Override
    public String toString() {
      return "AuditLogEvent{" +
          "name='" + name + '\'' +
          ", timestamp=" + timestamp +
          ", type='" + type + '\'' +
          ", host='" + host + '\'' +
          ", oldSource='" + oldSource + '\'' +
          ", oldOrigin='" + oldOrigin + '\'' +
          ", oldValue='" + oldValue + '\'' +
          ", newSource='" + newSource + '\'' +
          ", newOrigin='" + newOrigin + '\'' +
          ", newValue='" + newValue + '\'' +
          '}';
    }
  }
}
