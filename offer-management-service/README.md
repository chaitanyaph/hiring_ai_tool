# Offer Management Service

Owns the full offer lifecycle after a candidate clears all interview rounds: draft, single-stage approval, PDF generation, send, candidate accept/decline/negotiate, expiry, activity history.

## Figma reality check (read first)

Real Figma coverage is thinner than the spec's module list — this build matches the Figma exactly where it exists, and flags clearly where it doesn't:
- **5 status tabs**, not 8 (All/Pending approval/Sent/Accepted/Declined) — Draft and Withdrawn exist as *actions* (wizard's "Save as draft", drawer's "Withdraw" button) but have no dedicated tab; Expired has neither a tab nor a button, added purely for the "Offer Expiry" functional requirement.
- **Single-stage approval only** — one approver chip selected at creation, one combined "Approve & send" action. No HR/Hiring-Manager/Finance 3-tier workflow exists anywhere in the mockup.
- **3-field compensation model** (base salary, variable/bonus, ESOP/equity → computed total CTC) plus a fixed 4-item benefits checklist. Joining bonus, retention bonus, deductions, allowances, reporting manager, probation period, and notice buyout are **confirmed absent** (grep-verified zero hits) and are not built.
- **Zero negotiation UI anywhere** — no Negotiate button on the candidate side, no negotiation display on the recruiter side (grep-verified zero hits for "negotiat" in the whole mockup). `offer_negotiation` and the `request-negotiation` endpoint are still built, because they're explicitly named in the required Candidate API list — flagged here as a case where your own literal API list and your own "Figma only" instruction are in direct tension; I resolved it in favor of the explicit literal ask, built minimally, and am flagging it prominently rather than silently picking one instruction over the other.
- **No offer-version-history UI and no clone action** exist in the Figma or the literal API list — `offer_version` is not built at all (not a "richer text spec" case; both sources agree it's out of scope).

## Database Design — `offer_management_db`

Consolidated from the suggested 13 tables to **4** (see `V1__init_offer_management_schema.sql` header for full per-table reasoning):

| Table | Absorbs |
|---|---|
| `offer` | Aggregate root. Absorbs `offer_status`, `offer_salary` + `offer_component`, `candidate_offer`, `joining_details`, `approval_workflow` (single approver/status/notes columns, no 3-tier table). |
| `offer_document` | Generated PDF metadata + bytes, inline `LONGBLOB` (no object-storage client requested for this service, same reasoning as notification-service's email attachments). |
| `offer_negotiation` | Candidate negotiation requests — zero Figma coverage, see above. |
| `offer_activity_log` | Absorbs `offer_history` + `approval_history` — one append-only table backs both the recruiter drawer's timeline and the `/history` endpoint. |

## The real integration win: `offer.offer.released`

`application-service` already has a live `@KafkaListener` (`onOfferReleased`) waiting on topic `offer.offer.released` with payload `{applicationId, offerId}` — confirmed by reading its actual consumer code. It only advances status when the application is currently in `BACKGROUND_VERIFICATION`. This service publishes onto that exact topic the moment an offer is sent (`OfferApprovalService.doSend`), which is the real mechanism that unblocks the applicant pipeline — not just a forward-scaffolded event nobody listens to.

Similarly, `OfferAccepted`/`OfferRejected` are bridged onto `application.offer.accepted` / `application.offer.rejected` — application-service's own existing candidate-driven-accept/reject topic shape (`{applicationId, companyId, jobId, candidateId, occurredAt}`), also already consumed by `notification-service`'s `OFFER_ACCEPTED`/`OFFER_REJECTED` email templates. **Note**: application-service still has its own candidate-facing `POST /api/v1/applications/{id}/accept-offer`/`reject-offer` endpoints — this service's candidate portal is a second, parallel path to the same outcome. Both are left in place (out of scope to modify application-service); bridging onto the same topic/shape ensures the downstream pipeline advances correctly regardless of which UI the candidate actually uses.

## Kafka Event Flow

**Consumed:** `interview-management.candidate.selected` — confirmed to have had **zero consumers anywhere** before this service (interview-management-service's own topic comment flags it as forward-scaffolded). This service is its first real subscriber.

**Published, own (forward-scaffolded):** `OfferGenerated`, `OfferApproved`, `OfferSent`, `OfferAccepted`, `OfferRejected`, `OfferNegotiationRequested`, `CandidateOnboardingStarted`.

**Published, bridged onto already-live consumers:** `offer.offer.released`, `application.offer.accepted`, `application.offer.rejected` (see above).

## OpenFeign Communication

| Client | Purpose |
|---|---|
| `CandidateServiceClient` | name/email enrichment |
| `CompanyServiceClient` | company name enrichment |
| `ApplicationServiceClient` | job title / candidate snapshot enrichment (job-scoped list only — no single-application internal endpoint exists on application-service) |
| `InterviewManagementServiceClient` | optional context; auth-protected endpoint, safe-degrade pattern |

**Not built:** a Notification Service client (its only endpoints require a real user JWT — no internal/M2M controller exists there, confirmed by reading its actual controllers — so a backend-to-backend call would have no working use case; integration is entirely the Kafka publishes above) and a Background Verification Service client (**that service doesn't exist anywhere on disk**, confirmed by directory listing).

## PDF Generation

Uses **OpenPDF 1.3.8** — the only PDF-generation library confirmed cached in this offline build environment (iText isn't available; PDFBox 3.0.3 is also cached but OpenPDF's table/paragraph API suits a letter document better). Company logo is a text placeholder (company name only) — no image-asset pipeline exists for this service.

## Build environment note

This is an offline-only build environment (no network access to Maven Central, confirmed in prior sessions). All dependencies used here (`com.github.librepdf:openpdf:1.3.8`, `spring-boot-starter-thymeleaf` equivalents from prior services, etc.) were verified present in the local `.m2` repository before use.

## Running locally

```
docker compose up -d
```

Or standalone: `mvn spring-boot:run` (requires MySQL on `3306`, Redis on `6379`, Kafka on `9092`).
