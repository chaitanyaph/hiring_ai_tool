package com.cadence.offermanagementservice.controller;

import com.cadence.offermanagementservice.dto.request.CandidateDeclineRequest;
import com.cadence.offermanagementservice.dto.request.CandidateNegotiationRequest;
import com.cadence.offermanagementservice.dto.response.ApiResponse;
import com.cadence.offermanagementservice.dto.response.CandidateOfferResponse;
import com.cadence.offermanagementservice.security.CurrentUserProvider;
import com.cadence.offermanagementservice.service.CandidateOfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Backs #csec-offers ("My offer"), #modal-accept-offer, #modal-decline-offer. */
@RestController
@RequestMapping("/api/v1/candidate/offers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Candidate Offers", description = "View, download, accept, decline, and negotiate offers")
public class CandidateOfferController {

    private final CandidateOfferService candidateOfferService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @Operation(summary = "List my offers")
    public ResponseEntity<ApiResponse<List<CandidateOfferResponse>>> list() {
        var candidateId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("OK", candidateOfferService.listMyOffers(candidateId)));
    }

    @GetMapping("/{offerId}")
    @Operation(summary = "Get my offer detail")
    public ResponseEntity<ApiResponse<CandidateOfferResponse>> get(@PathVariable UUID offerId) {
        var candidateId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("OK", candidateOfferService.getMyOffer(candidateId, offerId)));
    }

    @GetMapping("/{offerId}/download")
    @Operation(summary = "Download my offer letter")
    public ResponseEntity<byte[]> download(@PathVariable UUID offerId) {
        var candidateId = currentUserProvider.getCurrentUser().getUserId();
        byte[] pdf = candidateOfferService.downloadMyOffer(candidateId, offerId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"offer-letter.pdf\"")
                .body(pdf);
    }

    @PostMapping("/{offerId}/accept")
    @Operation(summary = "Accept the offer (checkbox-gated on the frontend)")
    public ResponseEntity<ApiResponse<CandidateOfferResponse>> accept(@PathVariable UUID offerId) {
        var candidateId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("Offer accepted — welcome aboard!", candidateOfferService.accept(candidateId, offerId)));
    }

    @PostMapping("/{offerId}/reject")
    @Operation(summary = "Decline the offer")
    public ResponseEntity<ApiResponse<CandidateOfferResponse>> reject(@PathVariable UUID offerId, @RequestBody(required = false) CandidateDeclineRequest request) {
        var candidateId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("Offer declined", candidateOfferService.reject(candidateId, offerId, request)));
    }

    @PostMapping("/{offerId}/request-negotiation")
    @Operation(summary = "Request a negotiation on the offer terms")
    public ResponseEntity<ApiResponse<Void>> requestNegotiation(@PathVariable UUID offerId, @RequestBody(required = false) CandidateNegotiationRequest request) {
        var candidateId = currentUserProvider.getCurrentUser().getUserId();
        candidateOfferService.requestNegotiation(candidateId, offerId, request);
        return ResponseEntity.ok(ApiResponse.ok("Negotiation request sent"));
    }
}
