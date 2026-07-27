package com.cadence.offermanagementservice.controller;

import com.cadence.offermanagementservice.constants.OfferStatus;
import com.cadence.offermanagementservice.constants.PlatformRole;
import com.cadence.offermanagementservice.dto.request.ApproveOfferRequest;
import com.cadence.offermanagementservice.dto.request.CreateOrUpdateOfferRequest;
import com.cadence.offermanagementservice.dto.request.WithdrawOfferRequest;
import com.cadence.offermanagementservice.dto.response.*;
import com.cadence.offermanagementservice.security.CurrentUserProvider;
import com.cadence.offermanagementservice.service.OfferApprovalService;
import com.cadence.offermanagementservice.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Backs #sec-offers (dashboard KPIs, tabs, table), #modal-offer (4-step wizard), #drawer-offer-detail. */
@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Recruiter Offers", description = "Create/update/delete/list/detail offers, generate/preview letters, approve/send/withdraw")
public class RecruiterOfferController {

    private final OfferService offerService;
    private final OfferApprovalService offerApprovalService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Create an offer (wizard steps 1-3), then apply step 4's send mode (draft/approval/send)")
    public ResponseEntity<ApiResponse<OfferDetailResponse>> create(@Valid @RequestBody CreateOrUpdateOfferRequest request) {
        var user = currentUserProvider.getCurrentUser();
        var created = offerService.createOffer(user.getCompanyId(), user.getUserId(), request);
        var response = applySendMode(user.getCompanyId(), created.getId(), user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(sendModeMessage(request), response));
    }

    @PutMapping("/{offerId}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Update a draft or pending-approval offer")
    public ResponseEntity<ApiResponse<OfferDetailResponse>> update(@PathVariable UUID offerId, @Valid @RequestBody CreateOrUpdateOfferRequest request) {
        var user = currentUserProvider.getCurrentUser();
        var updated = offerService.updateOffer(user.getCompanyId(), offerId, request);
        var response = applySendMode(user.getCompanyId(), updated.getId(), user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Offer updated", response));
    }

    @DeleteMapping("/{offerId}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Delete a draft offer")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID offerId) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        offerService.deleteDraft(companyId, offerId);
        return ResponseEntity.ok(ApiResponse.ok("Draft offer deleted"));
    }

    @GetMapping
    @Operation(summary = "List offers (dashboard tabs: All/Pending approval/Sent/Accepted/Declined)")
    public ResponseEntity<ApiResponse<PagedResponse<OfferListItemResponse>>> list(
            @RequestParam(required = false) OfferStatus status, @PageableDefault(size = 20) Pageable pageable) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", offerService.listOffers(companyId, status, pageable)));
    }

    @GetMapping("/{offerId}")
    @Operation(summary = "Get offer detail (drawer-offer-detail)")
    public ResponseEntity<ApiResponse<OfferDetailResponse>> get(@PathVariable UUID offerId) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", offerService.getOffer(companyId, offerId)));
    }

    @PostMapping("/{offerId}/generate")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Generate the offer letter PDF")
    public ResponseEntity<byte[]> generate(@PathVariable UUID offerId) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        byte[] pdf = offerService.generateDocument(companyId, offerId);
        return pdfResponse(pdf, "offer-letter.pdf");
    }

    @PostMapping("/{offerId}/preview")
    @Operation(summary = "Preview the offer letter without persisting a document record")
    public ResponseEntity<byte[]> preview(@PathVariable UUID offerId) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        byte[] pdf = offerService.previewDocument(companyId, offerId);
        return pdfResponse(pdf, "offer-letter-preview.pdf");
    }

    @GetMapping("/{offerId}/document")
    @Operation(summary = "Download the most recently generated offer letter")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable UUID offerId) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        byte[] pdf = offerService.downloadDocument(companyId, offerId);
        return pdfResponse(pdf, "offer-letter.pdf");
    }

    @PostMapping("/{offerId}/approve")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Approve or reject-approval an offer (drawer's Reject / Approve & send)")
    public ResponseEntity<ApiResponse<OfferDetailResponse>> approve(@PathVariable UUID offerId, @Valid @RequestBody ApproveOfferRequest request) {
        var user = currentUserProvider.getCurrentUser();
        boolean isCompanyAdmin = PlatformRole.COMPANY_ADMIN.equals(user.getRole());
        var response = offerApprovalService.approve(user.getCompanyId(), offerId, user.getUserId(), isCompanyAdmin, request);
        return ResponseEntity.ok(ApiResponse.ok(request.isApprove() ? "Offer approved and sent to candidate" : "Offer approval rejected", response));
    }

    @PostMapping("/{offerId}/send")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Send the offer to the candidate, or resend a reminder if already sent")
    public ResponseEntity<ApiResponse<OfferDetailResponse>> send(@PathVariable UUID offerId) {
        var user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok("Offer sent to candidate", offerApprovalService.send(user.getCompanyId(), offerId, user.getUserId())));
    }

    @PostMapping("/{offerId}/withdraw")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Withdraw a sent offer")
    public ResponseEntity<ApiResponse<OfferDetailResponse>> withdraw(@PathVariable UUID offerId, @RequestBody(required = false) WithdrawOfferRequest request) {
        var user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok("Offer withdrawn", offerApprovalService.withdraw(user.getCompanyId(), offerId, user.getUserId(), request)));
    }

    @GetMapping("/history")
    @Operation(summary = "Get full offer history (unfiltered)")
    public ResponseEntity<ApiResponse<PagedResponse<OfferListItemResponse>>> history(@PageableDefault(size = 20) Pageable pageable) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", offerService.listOffers(companyId, null, pageable)));
    }

    @GetMapping("/pending")
    @Operation(summary = "List offers pending approval")
    public ResponseEntity<ApiResponse<PagedResponse<OfferListItemResponse>>> pending(@PageableDefault(size = 20) Pageable pageable) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", offerService.listOffers(companyId, OfferStatus.PENDING_APPROVAL, pageable)));
    }

    @GetMapping("/accepted")
    @Operation(summary = "List accepted offers")
    public ResponseEntity<ApiResponse<PagedResponse<OfferListItemResponse>>> accepted(@PageableDefault(size = 20) Pageable pageable) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", offerService.listOffers(companyId, OfferStatus.ACCEPTED, pageable)));
    }

    @GetMapping("/rejected")
    @Operation(summary = "List declined offers")
    public ResponseEntity<ApiResponse<PagedResponse<OfferListItemResponse>>> rejected(@PageableDefault(size = 20) Pageable pageable) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", offerService.listOffers(companyId, OfferStatus.DECLINED, pageable)));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard KPI stats (Offers sent / Acceptance rate / Avg time to accept / Pending approval)")
    public ResponseEntity<ApiResponse<OfferDashboardStatsResponse>> stats() {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", offerService.getDashboardStats(companyId)));
    }

    private OfferDetailResponse applySendMode(UUID companyId, UUID offerId, UUID recruiterId, CreateOrUpdateOfferRequest request) {
        return switch (request.getSendMode()) {
            case APPROVAL -> offerApprovalService.submitForApproval(companyId, offerId, recruiterId);
            case SEND -> offerApprovalService.send(companyId, offerId, recruiterId);
            case DRAFT -> offerService.getOffer(companyId, offerId);
        };
    }

    private String sendModeMessage(CreateOrUpdateOfferRequest request) {
        return switch (request.getSendMode()) {
            case DRAFT -> "Offer saved as draft";
            case APPROVAL -> "Offer sent for approval";
            case SEND -> "Offer sent to candidate";
        };
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(pdf);
    }
}
