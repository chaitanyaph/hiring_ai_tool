package com.cadence.offermanagementservice.constants;

public final class KafkaTopics {
    private KafkaTopics() {}

    // ---- Consumed -- owned/published by interview-management-service.
    // Confirmed by research to have had ZERO consumers before this
    // service -- offer-management-service is its first real subscriber.
    public static final String CANDIDATE_SELECTED = "interview-management.candidate.selected";

    // ---- Published by this service, forward-scaffolded (no consumer exists anywhere yet) ----
    public static final String OFFER_GENERATED = "offer-management.offer.generated";
    public static final String OFFER_APPROVED = "offer-management.offer.approved";
    public static final String OFFER_SENT = "offer-management.offer.sent";
    public static final String OFFER_ACCEPTED = "offer-management.offer.accepted";
    public static final String OFFER_REJECTED = "offer-management.offer.rejected";
    public static final String OFFER_NEGOTIATION_REQUESTED = "offer-management.offer.negotiation-requested";
    public static final String CANDIDATE_ONBOARDING_STARTED = "offer-management.candidate.onboarding-started";

    // ---- Bridged onto already-live consumers (the real integration value) ----
    // EXACT topic + shape application-service's own ApplicationEventConsumer
    // already actively deserializes (onOfferReleased) -- only transitions
    // status when current status is exactly BACKGROUND_VERIFICATION.
    // Do not rename, this is load-bearing on the other side.
    public static final String APPLICATION_OFFER_RELEASED = "offer.offer.released";
    // EXACT topic + shape application-service publishes today (candidate-
    // driven accept/reject on application-service's own endpoints) and
    // notification-service's OFFER_ACCEPTED/OFFER_REJECTED templates
    // already consume -- published here too so the pipeline advances
    // correctly regardless of which UI the candidate actually used.
    public static final String APPLICATION_OFFER_ACCEPTED = "application.offer.accepted";
    public static final String APPLICATION_OFFER_REJECTED = "application.offer.rejected";
}
