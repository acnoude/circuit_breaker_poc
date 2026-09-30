package com.example.circuitbreaker;

import org.springframework.stereotype.Component;

@Component
public class SlowIdentityProvider {
    public IdentityResult authenticate(long delayMs, boolean fail) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IdentityProviderException("Identity provider call interrupted", exception);
        }

        if (fail) {
            throw new IdentityProviderException("Simulated identity provider failure");
        }

        return new IdentityResult("authenticated", delayMs);
    }

    public record IdentityResult(String status, long providerDelayMs) {
    }

    public static class IdentityProviderException extends RuntimeException {
        public IdentityProviderException(String message) {
            super(message);
        }

        public IdentityProviderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}