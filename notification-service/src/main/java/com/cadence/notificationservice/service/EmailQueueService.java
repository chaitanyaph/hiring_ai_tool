package com.cadence.notificationservice.service;

import com.cadence.notificationservice.constants.EmailStatus;
import com.cadence.notificationservice.dto.request.RetryBulkRequest;
import com.cadence.notificationservice.dto.response.EmailDashboardStatsResponse;
import com.cadence.notificationservice.dto.response.EmailQueueDetailResponse;
import com.cadence.notificationservice.dto.response.EmailQueueItemResponse;
import com.cadence.notificationservice.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EmailQueueService {

    /** Backs notif-history (search + status filter). */
    PagedResponse<EmailQueueItemResponse> listEmails(String recipientSearch, EmailStatus status, Pageable pageable);

    EmailQueueDetailResponse getEmailDetail(UUID id);

    void retry(UUID id);

    void retryBulk(RetryBulkRequest request);

    /** Backs notif-scheduled's "Cancel" action. */
    void cancelScheduled(UUID id);

    /** Backs notif-dashboard KPI cards. */
    EmailDashboardStatsResponse getDashboardStats();
}
