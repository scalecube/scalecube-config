package io.scalecube.config.mongo;

import io.scalecube.config.audit.ConfigEvent;
import io.scalecube.config.audit.ConfigEventListener;

import java.util.Date;

import javax.annotation.Nonnull;

public class MongoConfigEventListener implements ConfigEventListener {
  private static final String DEFAULT_COLLECTION_NAME = "ConfigurationAuditLog";

  private final MongoConfigRepository repository;

  public MongoConfigEventListener(@Nonnull MongoConfigConnector connector) {
    this(connector, DEFAULT_COLLECTION_NAME);
  }

  public MongoConfigEventListener(@Nonnull MongoConfigConnector connector, @Nonnull String collectionName) {
    this.repository = new MongoConfigRepository(connector, collectionName);
  }

  @Override
  public void onEvent(ConfigEvent event) {
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
    repository.insertOneAsync(entity);
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
      return "AuditLogEntity{" +
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
