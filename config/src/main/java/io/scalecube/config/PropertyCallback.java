package io.scalecube.config;

import io.scalecube.config.utils.NameAndValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;

class PropertyCallback<T> implements BiConsumer<List<NameAndValue>, List<NameAndValue>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(PropertyCallback.class);

  private final Function<List<NameAndValue>, Object> valueParser;
  private final Collection<BiConsumer<T, T>> callbacks = new CopyOnWriteArraySet<>();

  PropertyCallback(Function<List<NameAndValue>, Object> valueParser) {
    this.valueParser = valueParser;
  }

  void addCallback(BiConsumer<T, T> callback) {
    callbacks.add(callback);
  }

  void addCallback(Executor executor, BiConsumer<T, T> callback) {
    callbacks.add((t1, t2) -> executor.execute(() -> invokeCallback(callback, t1, t2)));
  }

  @Override
  public void accept(List<NameAndValue> oldValues, List<NameAndValue> newValues) {
    T t1 = null;
    if (!oldValues.isEmpty()) {
      try {
        // noinspection unchecked
        t1 = (T) valueParser.apply(oldValues);
      } catch (Exception e) {
        LOGGER.error("Exception occured at valueParser on oldValue: {}, cause: {}", oldValues, e);
      }
    }

    T t2 = null;
    if (!newValues.isEmpty()) {
      try {
        // noinspection unchecked
        t2 = (T) valueParser.apply(newValues);
      } catch (Exception e) {
        LOGGER.error("Exception occured at valueParser on newValue: {}, cause: {}", newValues, e);
      }
    }

    for (BiConsumer<T, T> callback : callbacks) {
      invokeCallback(callback, t1, t2);
    }
  }

  private void invokeCallback(BiConsumer<T, T> callback, T t1, T t2) {
    try {
      callback.accept(t1, t2);
    } catch (Exception e) {
      LOGGER.error("Exception occurred on property-change callback: {}, oldValue: {}, newValue: {}, cause: {}",
          callback, t1, t2, e, e);
    }
  }
}
