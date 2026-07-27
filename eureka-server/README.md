# Service Registry (Eureka Server)

Netflix Eureka Server for the Cadence platform — the single service registry every one of the 14 other services (13 business services + the API Gateway) already registers with. This closes the loop on the whole platform's discovery/routing story: the Gateway's `lb://<service-id>` routes and every `@FeignClient(name = "...")` across all 13 business services were already built to resolve against Eureka; they just had nothing real listening on port 8761 until this service existed.

## Prerequisite gap found and fixed while building this

All 14 client services had `eureka.client.enabled: false` hardcoded in their `application.yml` — a copy-paste template default that was flipped to `true` in an earlier session (see each service's `application.yml`), but three things were still missing that this task adds:

1. **`eureka.client.healthcheck.enabled: true`** on all 14 clients — without it, Eureka only knows "alive" from heartbeats, not real Actuator health. A service that's process-alive but DB-disconnected would be falsely reported `UP`.
2. **A collision-safe `eureka.instance.instance-id`** (`${spring.application.name}:${spring.application.instance_id:${random.value}}`) on all 14 clients — needed for correct multi-instance registration when scaling any service to multiple replicas.
3. **Basic-auth credentials embedded in every client's `EUREKA_URI`** (`http://eureka:eureka_pass@...`) — this Eureka server requires authentication (see below); the 14 clients' `application.yml` defaults and all 13 existing services' `docker-compose.yml` `EUREKA_URI` env vars were updated to match.

## Security

An unauthenticated Eureka server on a shared network lets anyone browse every registered instance of every service, or deregister them. `SecurityConfig` requires HTTP Basic auth on everything except `/actuator/health` and `/actuator/info` (kept open for the Docker healthcheck and orchestrator probes), including `/eureka/**` itself — every client's `defaultZone` URL carries the same credentials embedded, which Spring Cloud Netflix's Eureka client supports natively. CSRF is disabled only for `/eureka/**` (Eureka clients never send a CSRF token); the dashboard itself keeps CSRF protection. Credentials default to `eureka` / `eureka_pass` (both overridable via `EUREKA_USERNAME` / `EUREKA_PASSWORD`) — **change these in any real deployment**, they're dev-only defaults matching the pattern every sibling service uses for its own dev-only secrets (e.g. the shared JWT signing key).

## High availability

Two-node peer-aware cluster via Spring profiles (`application-peer1.yml` / `application-peer2.yml`) — each node registers as a Eureka *client* of the other, so registry state replicates both ways. `docker-compose.yml` runs both (`eureka-peer1` on host port 8761, `eureka-peer2` on 8762). For local single-instance dev, run without a profile (the base `application.yml`'s `register-with-eureka: false` / `fetch-registry: false` standalone defaults) instead of the HA compose file.

## Self-preservation and lease tuning

Self-preservation stays **on** by default (`eureka.server.enable-self-preservation: true`) — this is the correct setting for a real deployment: it stops the server from mass-evicting every instance during a network partition (a renewal-rate drop across *all* clients, not one dead instance). Renewal/expiration on the client side use the standard values (`lease-renewal-interval-in-seconds: 30`, `lease-expiration-duration-in-seconds: 90`), set explicitly on all 14 clients rather than left implicit, for clarity and easy tuning.

## Known gap, flagged: per-service docker-compose fragmentation

Every one of the 13 existing business services' own `docker-compose.yml` still contains its own **generic placeholder** `eureka-server` container block (`image: steeltoeoss/eureka-server:latest`), from before this real `eureka-server` module existed and before Eureka registration was even enabled. Running any one of those 13 compose files in isolation still spins up its own separate, unauthenticated, single-purpose Eureka instance — not this real, secured, HA-capable one. Full platform integration requires either:
- Retrofitting each of those 13 `docker-compose.yml` files to depend on this service's containers over a shared external Docker network instead of their own embedded placeholder, or
- A single top-level compose file (not built anywhere in this platform yet) that composes all 15 services together.

Not fixed in this task — flagged here rather than silently left as a trap, since it's a real, mechanical, somewhat separate follow-up (13 file edits) rather than part of building the registry itself.

## Build environment note

Offline-only build environment, same constraint as every other service in this platform. `spring-cloud-starter-netflix-eureka-server`'s own transitive dependencies (Jersey, used internally for peer replication; Freemarker, used for the dashboard's HTML templates) needed two version overrides to resolve against the local `.m2` cache — see the `pom.xml` comments for exact versions and why.

## Running locally

```
docker compose up -d
```

Dashboard: `http://localhost:8761` (peer1) or `http://localhost:8762` (peer2), basic auth `eureka` / `eureka_pass`.

Or standalone: `mvn spring-boot:run` (no external dependencies required — this service has none besides itself).
