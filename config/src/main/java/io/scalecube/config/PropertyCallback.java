package io.scalecube.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;

class PropertyCallback<T> implements BiConsumer<String, String> {
  private static final Logger LOGGER = LoggerFactory.getLogger(PropertyCallback.class);

  private final Function<String, Object> valueParser;
  private final Collection<BiConsumer<T, T>> callbacks = new CopyOnWriteArrayList<>();

  PropertyCallback(Function<String, Object> valueParser) {
    this.valueParser = valueParser;
  }

  void addCallback(BiConsumer<T, T> callback) {
    callbacks.add((t1, t2) -> {
      try {
        callback.accept(t1, t2);
      } catch (Exception e) {
        LOGGER.error("Exception occurred on property-change callback: {}, oldValue={}, newValue={}, cause: {}",
            callback, t1, t2, e, e);
      }
    });
  }

  void addCallback(Executor executor, BiConsumer<T, T> callback) {
    callbacks.add((t1, t2) -> executor.execute(() -> {
      try {
        callback.accept(t1, t2);
      } catch (Exception e) {
        LOGGER.error("Exception occurred on property-change callback: {}, oldValue={}, newValue={}, cause: {}",
            callback, t1, t2, e, e);
      }
    }));
  }

  @Override
  public void accept(String s1, String s2) {
    T t1 = null;
    Exception e1 = null;
    try {
      // noinspection unchecked
      t1 = s1 != null ? (T) valueParser.apply(s1) : null;
    } catch (Exception e) {
      LOGGER.error("Exception occured on valueParser: '{}', cause: {}", s1, e);
      e1 = e; // old value parsing error
    }

    T t2 = null;
    Exception e2 = null;
    try {
      // noinspection unchecked
      t2 = s2 != null ? (T) valueParser.apply(s2) : null;
    } catch (Exception e) {
      LOGGER.error("Exception occured at valueParser: '{}', cause: {}", s2, e);
      e2 = e;
    }

    if (e1 != null || e2 != null) {
      return; // parsing failed either on old value or on new value
    }

    for (BiConsumer<T, T> callback : callbacks) {
      callback.accept(t1, t2);
    }
  }
}
