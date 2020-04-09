package uk.gov.justice.hmpps.datacompliance.utils;

import lombok.NoArgsConstructor;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class Exceptions {

    public static <T> T propagateAnyError(final Callable<T> callable) {
        try {
            return callable.call();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Supplier<IllegalStateException> illegalState(final String message, final Object... args) {
        return () -> new IllegalStateException(format(message, args));
    }
}
