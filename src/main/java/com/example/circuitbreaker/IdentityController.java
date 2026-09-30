package com.example.circuitbreaker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IdentityController {
    private final IdentityService identityService;

    public IdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @GetMapping("/identity")
    public CompletableFuture<ResponseEntity<?>> authenticate(
            @RequestParam(defaultValue = "2000") long delayMs,
            @RequestParam(defaultValue = "false") boolean fail) {
        long boundedDelayMs = Math.max(0, Math.min(delayMs, 30_000));
        return identityService.authenticate(boundedDelayMs, fail)
                .<ResponseEntity<?>>handle((result, error) -> {
                    if (error == null) {
                        return ResponseEntity.ok(result);
                    }
                    Throwable cause = unwrap(error);
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .header("Retry-After", "1")
                            .body(new UnavailableResponse("unavailable", cause.getClass().getSimpleName(),
                                    cause.getMessage()));
                });
    }

    private Throwable unwrap(Throwable error) {
        Throwable cause = error;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    public record UnavailableResponse(String status, String reason, String message) {
    }
}