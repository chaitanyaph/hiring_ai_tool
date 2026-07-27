package com.cadence.offermanagementservice.service;

import com.cadence.offermanagementservice.dto.request.ApproveOfferRequest;
import com.cadence.offermanagementservice.dto.request.WithdrawOfferRequest;
import com.cadence.offermanagementservice.dto.response.OfferDetailResponse;

import java.util.UUID;

/** Offer lifecycle transitions (§12 approval flow): submit-for-approval, approve/reject, send, withdraw. */
public interface OfferApprovalService {

    OfferDetailResponse submitForApproval(UUID companyId, UUID offerId, UUID recruiterId);

    /** Matches the Figma's combined "Approve & send" single action -- approve=true moves straight to SENT (publishes OfferApproved + OfferSent + the offer.offer.released bridge). */
    OfferDetailResponse approve(UUID companyId, UUID offerId, UUID callerId, boolean callerIsCompanyAdmin, ApproveOfferRequest request);

    /** DRAFT/PENDING_APPROVAL -> SENT directly (wizard's "Send to candidate" mode), or a no-op "resend reminder" log entry if already SENT. */
    OfferDetailResponse send(UUID companyId, UUID offerId, UUID recruiterId);

    OfferDetailResponse withdraw(UUID companyId, UUID offerId, UUID recruiterId, WithdrawOfferRequest request);
}
