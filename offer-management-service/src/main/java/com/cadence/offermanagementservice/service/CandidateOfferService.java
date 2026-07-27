package com.cadence.offermanagementservice.service;

import com.cadence.offermanagementservice.dto.request.CandidateDeclineRequest;
import com.cadence.offermanagementservice.dto.request.CandidateNegotiationRequest;
import com.cadence.offermanagementservice.dto.response.CandidateOfferResponse;

import java.util.List;
import java.util.UUID;

public interface CandidateOfferService {

    List<CandidateOfferResponse> listMyOffers(UUID candidateId);

    CandidateOfferResponse getMyOffer(UUID candidateId, UUID offerId);

    byte[] downloadMyOffer(UUID candidateId, UUID offerId);

    CandidateOfferResponse accept(UUID candidateId, UUID offerId);

    CandidateOfferResponse reject(UUID candidateId, UUID offerId, CandidateDeclineRequest request);

    /** Zero Figma UI coverage -- see README. */
    void requestNegotiation(UUID candidateId, UUID offerId, CandidateNegotiationRequest request);
}
