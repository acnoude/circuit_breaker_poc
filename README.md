# Slow Identity Provider Resilience Demo

A standalone Spring Boot app that simulates a slow third-party identity provider and isolates calls with Resilience4j.

## Run

Requires Java 17 or newer and Maven.

```sh
mvn spring-boot:run
```

`GET /identity` simulates a successful provider call. `delayMs` defaults to 2000 and is capped at 30000. Pass `fail=true` to simulate a provider failure.

```sh
curl -i 'http://localhost:8080/identity?delayMs=500'
curl -i 'http://localhost:8080/identity?delayMs=50&fail=true'
```

Successful calls return `200`; provider failures, open-circuit calls, and bulkhead rejections return `503` immediately.

## Isolation

The identity provider has a dedicated four-thread `ThreadPoolBulkhead` with no queue. The servlet request threads are released while each call runs asynchronously. The circuit breaker is inside the bulkhead so overload rejections do not count as provider failures. Three provider failures in a three-call window open the breaker for ten seconds.

To send a parallel burst while the app is running:

```sh
seq 1 12 | xargs -P12 -I{} curl -sS -o /dev/null -w '%{http_code} %{time_total}s\n' \
	'http://localhost:8080/identity?delayMs=1200'
```

At most four requests run at once. The excess requests should return `503` without waiting for the 1.2-second provider delay.

## Automated Proof

```sh
mvn test
```

The overload test submits 12 calls to the four-worker, zero-queue bulkhead, asserts that eight are rejected and that submission finishes in under 500 ms, and waits for accepted calls to finish. A second test confirms that three provider failures open the circuit and the next call is rejected.