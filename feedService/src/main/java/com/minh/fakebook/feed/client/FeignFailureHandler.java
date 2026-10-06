package com.minh.fakebook.feed.client;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

final class FeignFailureHandler {

    private FeignFailureHandler() {}

    static void rethrowClientError(Throwable cause) {
        Throwable actualCause = unwrap(cause);
        if (actualCause instanceof FeignException.FeignClientException clientException) {
            throw clientException;
        }
    }

    static boolean isCircuitOpen(Throwable cause) {
        return unwrap(cause) instanceof CallNotPermittedException;
    }

    private static Throwable unwrap(Throwable cause) {
        Throwable current = cause;
        while (
            current.getCause() != null &&
            (current instanceof CompletionException || current instanceof ExecutionException)
        ) {
            current = current.getCause();
        }
        return current;
    }
}
