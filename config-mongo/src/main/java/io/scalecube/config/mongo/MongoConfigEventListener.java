package io.scalecube.config.mongo;

import io.scalecube.config.audit.AuditConfigEvent;
import io.scalecube.config.audit.ConfigEventListener;
import io.scalecube.config.utils.ThrowableUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.bson.RawBsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

public class MongoConfigEventListener implements ConfigEventListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(MongoConfigEventListener.class);

  private static final ThreadFactory threadFactory;
  static {
    threadFactory = r -> {
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

  public MongoConfigEventListener(@Nonnull MongoConfigConnector connector, @Nonnull String collectionName) {
    this.connector = connector;
    this.collectionName = collectionName;
  }

  @Override
  public void onEvents(Collection<AuditConfigEvent> events) {
    CompletableFuture.runAsync(() -> {
      ObjectMapper objectMapper = MongoConfigObjectMapper.getInstance();
      connector.getDatabase().getCollection(collectionName, RawBsonDocument.class)
          .insertMany(events.stream()
              .map(event -> {
                AuditLogEntity entity = new AuditLogEntity();
                entity.setSource(event.getSource());
                entity.setOrigin(event.getOrigin());
                entity.setPropName(event.getPropName());
                entity.setPropValue(event.getPropValue());
                entity.setUpdateDate(event.getUpdateDate());
                return entity;
              })
              .map(entity -> {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                  objectMapper.writer().writeValue(baos, entity);
                } catch (Exception e) {
                  LOGGER.error("Exception at converting obj: {} to bson, cause: {}", entity, e);
                  throw ThrowableUtil.propagate(e);
                }
                return new RawBsonDocument(baos.toByteArray());
              })
              .collect(Collectors.toList()));
    }, executor);
  }

  private static class AuditLogEntity {

    private String source;
    private String origin;
    private String propName;
    private String propValue;
    private String updateDate;

    public String getSource() {
      return source;
    }

    public void setSource(String source) {
      this.source = source;
    }

    public String getOrigin() {
      return origin;
    }

    public void setOrigin(String origin) {
      this.origin = origin;
    }

    public String getPropName() {
      return propName;
    }

    public void setPropName(String propName) {
      this.propName = propName;
    }

    public String getPropValue() {
      return propValue;
    }

    public void setPropValue(String propValue) {
      this.propValue = propValue;
    }

    public String getUpdateDate() {
      return updateDate;
    }

    public void setUpdateDate(String updateDate) {
      this.updateDate = updateDate;
    }

    @Override
    public String toString() {
      return "AuditLogEntity{" +
          "source='" + source + '\'' +
          ", origin='" + origin + '\'' +
          ", propName='" + propName + '\'' +
          ", propValue='" + propValue + '\'' +
          ", updateDate='" + updateDate + '\'' +
          '}';
    }
  }
}
