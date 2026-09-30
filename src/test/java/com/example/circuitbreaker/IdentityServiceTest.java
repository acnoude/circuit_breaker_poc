package com.example.circuitbreaker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IdentityServiceTest {
    @Autowired
    private IdentityService identityService;

    @Test
    void overloadIsRejectedImmediatelyInsteadOfQueuingProviderWork() throws Exception {
        long startedAt = System.nanoTime();
        List<CompletionStage<SlowIdentityProvider.IdentityResult>> requests = new ArrayList<>();
        for (int request = 0; request < 12; request++) {
            requests.add(identityService.authenticate(1200, false));
        }

        long submissionTimeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        long rejected = requests.stream()
            .filter(request -> request.toCompletableFuture().isCompletedExceptionally())
            .count();

        assertThat(rejected).isEqualTo(8);
        assertThat(submissionTimeMs).isLessThan(500);
        for (CompletionStage<SlowIdentityProvider.IdentityResult> request : requests) {
            try {
                request.toCompletableFuture().get(3, TimeUnit.SECONDS);
            } catch (java.util.concurrent.ExecutionException ignored) {
                // The eight calls beyond the bulkhead capacity are expected to fail.
            }
        }
    }

    @Test
    void repeatedProviderFailuresOpenCircuitAndRejectTheNextCall() {
        for (int request = 0; request < 3; request++) {
            try {
                identityService.authenticate(0, true).join();
            } catch (CompletionException expected) {
                assertThat(expected.getCause())
                        .isInstanceOf(SlowIdentityProvider.IdentityProviderException.class);
            }
        }

        assertThatThrownBy(() -> identityService.authenticate(0, false).join())
                .hasCauseInstanceOf(CallNotPermittedException.class);
    }
}