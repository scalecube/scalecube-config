package io.scalecube.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * BiConsumer of list of {@link PropertyNameAndValue}. Implements generic logic of callback mechanism for all config
 * property types.
 *
 * @param <T> type parameter for {@link #valueParser} function
 */
class PropertyCallback<T> implements BiConsumer<List<PropertyNameAndValue>, List<PropertyNameAndValue>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(PropertyCallback.class);

  private final Function<List<PropertyNameAndValue>, T> valueParser;
  private final Collection<BiConsumer<T, T>> callbacks = new CopyOnWriteArraySet<>(); // Set based structure

  PropertyCallback(Function<List<PropertyNameAndValue>, T> valueParser) {
    this.valueParser = valueParser;
  }

  void addCallback(BiConsumer<T, T> callback) {
    callbacks.add(callback);
  }

  void addCallback(Executor executor, BiConsumer<T, T> callback) {
    callbacks.add((t1, t2) -> executor.execute(() -> invokeCallback(callback, t1, t2)));
  }

  @Override
  public void accept(List<PropertyNameAndValue> oldList, List<PropertyNameAndValue> newList) {
    T t1 = null;
    if (!oldList.isEmpty()) {
      try {
        // noinspection unchecked
        t1 = valueParser.apply(oldList);
      } catch (Exception e) {
        LOGGER.error("Exception occured at valueParser on oldValue: {}, cause: {}", oldList, e);
      }
    }

    T t2 = null;
    if (!newList.isEmpty()) {
      try {
        // noinspection unchecked
        t2 = valueParser.apply(newList);
      } catch (Exception e) {
        LOGGER.error("Exception occured at valueParser on newValue: {}, cause: {}", newList, e);
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
