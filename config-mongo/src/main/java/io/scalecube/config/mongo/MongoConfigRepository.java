package io.scalecube.config.mongo;

import io.scalecube.config.utils.ThrowableUtil;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mongodb.client.MongoCollection;

import org.bson.RawBsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

import de.undercouch.bson4jackson.BsonFactory;
import de.undercouch.bson4jackson.BsonModule;
import de.undercouch.bson4jackson.BsonParser;

public class MongoConfigRepository {
  private static final Logger LOGGER = LoggerFactory.getLogger(MongoConfigRepository.class);

  private static ThreadFactory threadFactory;
  static {
    threadFactory = r -> {
      Thread thread = new Thread(r);
      thread.setDaemon(true);
      thread.setName("mongo-config-repository");
      thread.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Exception occurred: " + e, e));
      return thread;
    };
  }

  private static final Executor mongoExecutor = Executors.newCachedThreadPool(threadFactory);

  private static final ObjectMapper objectMapper = new ObjectMapper(new BsonFactory()
      .enable(BsonParser.Feature.HONOR_DOCUMENT_LENGTH));

  static {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); // gives mongo type date in db
    objectMapper.registerModule(new BsonModule()); // gives mongo types in db
  }

  private final MongoConfigConnector connector;
  private final String collectionName;

  public MongoConfigRepository(@Nonnull MongoConfigConnector connector, @Nonnull String collectionName) {
    this.connector = Objects.requireNonNull(connector, "MongoConfigRepository: connector is required");
    this.collectionName = Objects.requireNonNull(collectionName, "MongoConfigRepository: collectionName is required");
  }

  public final <T> CompletableFuture<Void> insertOneAsync(T input) {
    return CompletableFuture.runAsync(() -> insertOne(input), mongoExecutor);
  }

  public final <T> CompletableFuture<Collection<T>> findAllAsync(@Nonnull Class<T> type) {
    return CompletableFuture.supplyAsync(() -> findAll(type), mongoExecutor);
  }

  private <T> void insertOne(T input) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      objectMapper.writer().writeValue(baos, input);
    } catch (IOException e) {
      LOGGER.error("Exception occurred at converting obj: {} to bson, cause: {}", input, e);
      throw ThrowableUtil.propagate(e);
    }
    getCollection().insertOne(new RawBsonDocument(baos.toByteArray()));
  }

  private <T> Collection<T> findAll(@Nonnull Class<T> type) {
    Objects.requireNonNull(type);
    return stream(getCollection().find().iterator()).map(doc -> {
      try {
        ByteArrayInputStream bin = new ByteArrayInputStream(doc.getByteBuffer().array());
        // noinspection unchecked
        return (T) objectMapper.readerFor(type).readValue(bin);
      } catch (Exception e) {
        LOGGER.error("Exception occurred at parsing bson to obj of type: {}, cause: {}", type, e);
        throw ThrowableUtil.propagate(e);
      }
    }).collect(Collectors.toList());
  }

  private <T> Stream<T> stream(Iterator<T> iterator) {
    return StreamSupport
        .stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.IMMUTABLE), false/* parallel */);
  }

  private MongoCollection<RawBsonDocument> getCollection() {
    return connector.getDatabase().getCollection(collectionName, RawBsonDocument.class);
  }
}
