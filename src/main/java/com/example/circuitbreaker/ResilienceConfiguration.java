package com.example.circuitbreaker;

import java.time.Duration;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfiguration {
    @Bean
    ThreadPoolBulkhead identityProviderBulkhead() {
        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
                .coreThreadPoolSize(4)
                .maxThreadPoolSize(4)
                .queueCapacity(0)
                .build();
        return ThreadPoolBulkhead.of("identity-provider", config);
    }

    @Bean
    CircuitBreaker identityProviderCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(3)
                .minimumNumberOfCalls(3)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .build();
        return CircuitBreaker.of("identity-provider", config);
    }
}