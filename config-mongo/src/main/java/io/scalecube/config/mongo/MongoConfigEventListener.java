package io.scalecube.config.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.scalecube.config.audit.ConfigEvent;
import io.scalecube.config.audit.ConfigEventListener;
import io.scalecube.config.utils.ThrowableUtil;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
import org.bson.RawBsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoConfigEventListener implements ConfigEventListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(MongoConfigEventListener.class);

  private static final ThreadFactory threadFactory;

  static {
    threadFactory =
        r -> {
          Thread thread = new Thread(r);
          thread.setDaemon(true);
          thread.setName("mongo-config-auditor");
          thread.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Exception occurred: " + e, e));
          return thread;
        };
  }

  private static final Executor executor = Executors.newSingleThreadExecutor(threadFactory);

  private final MongoConfigConnector connector;
  private final String collectionName;

  public MongoConfigEventListener(
       MongoConfigConnector connector,  String collectionName) {
    this.connector = connector;
    this.collectionName = collectionName;
  }

  @Override
  public void onEvents(Collection<ConfigEvent> events) {
    CompletableFuture.runAsync(
        () -> {
          ObjectMapper objectMapper = MongoConfigObjectMapper.getInstance();
          connector
              .getDatabase()
              .getCollection(collectionName, RawBsonDocument.class)
              .insertMany(
                  events
                      .stream()
                      .map(
                          event -> {
                            AuditLogEntity entity = new AuditLogEntity();
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
                            return entity;
                          })
                      .map(
                          entity -> {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            try {
                              objectMapper.writer().writeValue(baos, entity);
                            } catch (Exception e) {
                              LOGGER.error(
                                  "Exception at converting obj: {} to bson, cause: {}", entity, e);
                              throw ThrowableUtil.propagate(e);
                            }
                            return new RawBsonDocument(baos.toByteArray());
                          })
                      .collect(Collectors.toList()));
        },
        executor);
  }

  private static class AuditLogEntity {
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
      final StringBuilder sb = new StringBuilder("AuditLogEntity{");
      sb.append("name='").append(name).append('\'');
      sb.append(", timestamp=").append(timestamp);
      sb.append(", type='").append(type).append('\'');
      sb.append(", host='").append(host).append('\'');
      sb.append(", oldSource='").append(oldSource).append('\'');
      sb.append(", oldOrigin='").append(oldOrigin).append('\'');
      sb.append(", oldValue='").append(oldValue).append('\'');
      sb.append(", newSource='").append(newSource).append('\'');
      sb.append(", newOrigin='").append(newOrigin).append('\'');
      sb.append(", newValue='").append(newValue).append('\'');
      sb.append('}');
      return sb.toString();
    }
  }
}
