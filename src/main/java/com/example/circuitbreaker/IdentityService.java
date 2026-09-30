package com.example.circuitbreaker;

import java.util.concurrent.CompletableFuture;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.stereotype.Service;

@Service
public class IdentityService {
    private final ThreadPoolBulkhead bulkhead;
    private final CircuitBreaker circuitBreaker;
    private final SlowIdentityProvider provider;

    public IdentityService(ThreadPoolBulkhead bulkhead, CircuitBreaker circuitBreaker,
                           SlowIdentityProvider provider) {
        this.bulkhead = bulkhead;
        this.circuitBreaker = circuitBreaker;
        this.provider = provider;
    }

    public CompletableFuture<SlowIdentityProvider.IdentityResult> authenticate(long delayMs, boolean fail) {
        try {
            return bulkhead.executeSupplier(
                    () -> circuitBreaker.executeSupplier(() -> provider.authenticate(delayMs, fail)))
                    .toCompletableFuture();
        } catch (BulkheadFullException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }
}