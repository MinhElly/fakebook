package com.minh.fakebook.comment.client;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

final class DownstreamFailureMapper {

    private DownstreamFailureMapper() {}

    static RuntimeException map(String serviceName, String operation, Throwable cause) {
        Throwable actualCause = unwrap(cause);
        if (actualCause instanceof FeignException.FeignClientException clientException) {
            return new ResponseStatusException(
                HttpStatusCode.valueOf(clientException.status()),
                serviceName + " rejected request during " + operation,
                clientException
            );
        }
        return new DownstreamServiceUnavailableException(serviceName, operation, actualCause);
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
