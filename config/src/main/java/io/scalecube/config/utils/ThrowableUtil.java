package io.scalecube.config.utils;

import javax.annotation.Nullable;

public final class ThrowableUtil {

  private ThrowableUtil() {
    // Do not instantiate
  }

  /**
   * Propagates throwable as-is if throwable is instance of {@link RuntimeException} or {@link
   * Error}. In other case wraps into {@link RuntimeException}.
   *
   * @param throwable the throwable to be propagated
   * @return runtime exception
   */
  public static RuntimeException propagate(@Nullable Throwable throwable) {
    propagateIfInstanceOf(throwable, Error.class);
    propagateIfInstanceOf(throwable, RuntimeException.class);
    throw new RuntimeException(throwable);
  }

  private static <X extends Throwable> void propagateIfInstanceOf(
      Throwable throwable, Class<X> type) throws X {
    if (type.isInstance(throwable)) {
      throw type.cast(throwable);
    }
  }
}
