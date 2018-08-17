package io.scalecube.config.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import java.util.Objects;

public class MongoConfigConnector {
  private final MongoDatabase database;
  private final MongoClientURI clientUri;

  private MongoConfigConnector(Builder builder) {
    this.database = builder.database;
    this.clientUri = builder.clientUri;
  }

  public MongoDatabase getDatabase() {
    return database;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private MongoDatabase database;
    private MongoClientURI clientUri;

    /**
     * Creates builder for given URI.
     *
     * @param uri URI
     * @return builder instance
     */
    public Builder forUri(String uri) {
      this.clientUri = new MongoClientURI(uri);
      String databaseName = clientUri.getDatabase();
      Objects.requireNonNull(databaseName, "Mongo uri must contain database");
      this.database = new MongoClient(clientUri).getDatabase(databaseName);
      return this;
    }

    public MongoConfigConnector build() {
      return new MongoConfigConnector(this);
    }
  }

  @Override
  public String toString() {
    return "MongoConfigConnector{" + "database=" + clientUri + '}';
  }
}
