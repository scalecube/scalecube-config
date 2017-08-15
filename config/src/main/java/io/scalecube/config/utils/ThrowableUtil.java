package io.scalecube.config.utils;

import javax.annotation.Nullable;

public final class ThrowableUtil {

	private ThrowableUtil() {
		// Do not instantiate
	}

	public static RuntimeException propagate(@Nullable Throwable throwable) {
		propagateIfInstanceOf(throwable, Error.class);
		propagateIfInstanceOf(throwable, RuntimeException.class);
		throw new RuntimeException(throwable);
	}

	private static <X extends Throwable> void propagateIfInstanceOf(Throwable throwable, Class<X> type) throws X {
		if (throwable != null && type.isInstance(throwable)) {
			throw type.cast(throwable);
		}
	}
}
