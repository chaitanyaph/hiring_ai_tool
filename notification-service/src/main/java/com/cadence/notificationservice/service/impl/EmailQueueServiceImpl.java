package com.cadence.notificationservice.service.impl;

import com.cadence.notificationservice.constants.EmailStatus;
import com.cadence.notificationservice.dto.request.RetryBulkRequest;
import com.cadence.notificationservice.dto.response.EmailDashboardStatsResponse;
import com.cadence.notificationservice.dto.response.EmailQueueDetailResponse;
import com.cadence.notificationservice.dto.response.EmailQueueItemResponse;
import com.cadence.notificationservice.dto.response.PagedResponse;
import com.cadence.notificationservice.entity.EmailQueue;
import com.cadence.notificationservice.exception.ErrorCode;
import com.cadence.notificationservice.exception.ResourceNotFoundException;
import com.cadence.notificationservice.mapper.EmailQueueMapper;
import com.cadence.notificationservice.repository.EmailAttachmentRepository;
import com.cadence.notificationservice.repository.EmailQueueRepository;
import com.cadence.notificationservice.repository.NotificationTemplateRepository;
import com.cadence.notificationservice.service.EmailQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailQueueServiceImpl implements EmailQueueService {

    private final EmailQueueRepository emailQueueRepository;
    private final EmailAttachmentRepository emailAttachmentRepository;
    private final NotificationTemplateRepository templateRepository;
    private final EmailQueueMapper emailQueueMapper;

    @Override
    public PagedResponse<EmailQueueItemResponse> listEmails(String recipientSearch, EmailStatus status, Pageable pageable) {
        Page<EmailQueue> page;
        if (status != null && StringUtils.hasText(recipientSearch)) {
            page = emailQueueRepository.findAllByStatusAndRecipientEmailContainingIgnoreCase(status, recipientSearch, pageable);
        } else if (status != null) {
            page = emailQueueRepository.findAllByStatus(status, pageable);
        } else if (StringUtils.hasText(recipientSearch)) {
            page = emailQueueRepository.findAllByRecipientEmailContainingIgnoreCase(recipientSearch, pageable);
        } else {
            page = emailQueueRepository.findAll(pageable);
        }
        return PagedResponse.from(page.map(this::toItemResponseWithTemplate));
    }

    @Override
    public EmailQueueDetailResponse getEmailDetail(UUID id) {
        EmailQueue emailQueue = findEmail(id);
        EmailQueueDetailResponse response = emailQueueMapper.toDetailResponse(emailQueue);
        response.setAttachments(emailAttachmentRepository.findAllByEmailQueueId(id).stream()
                .map(a -> EmailQueueDetailResponse.AttachmentInfo.builder()
                        .id(a.getId()).fileName(a.getFileName()).contentType(a.getContentType()).sizeBytes(a.getSizeBytes())
                        .build())
                .toList());
        return response;
    }

    @Override
    @Transactional
    public void retry(UUID id) {
        EmailQueue emailQueue = findEmail(id);
        resetForRetry(emailQueue);
        emailQueueRepository.save(emailQueue);
    }

    @Override
    @Transactional
    public void retryBulk(RetryBulkRequest request) {
        List<EmailQueue> emails = emailQueueRepository.findAllByIdIn(request.getEmailQueueIds());
        emails.forEach(this::resetForRetry);
        emailQueueRepository.saveAll(emails);
    }

    @Override
    @Transactional
    public void cancelScheduled(UUID id) {
        EmailQueue emailQueue = findEmail(id);
        emailQueue.setStatus(EmailStatus.CANCELLED);
        emailQueue.setUpdatedAt(LocalDateTime.now());
        emailQueueRepository.save(emailQueue);
    }

    @Override
    public EmailDashboardStatsResponse getDashboardStats() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long sentToday = emailQueueRepository.countByStatusAndSentAtAfter(EmailStatus.SENT, startOfToday)
                + emailQueueRepository.countByStatusAndSentAtAfter(EmailStatus.DELIVERED, startOfToday)
                + emailQueueRepository.countByStatusAndSentAtAfter(EmailStatus.OPENED, startOfToday);
        long delivered = emailQueueRepository.countByStatus(EmailStatus.DELIVERED) + emailQueueRepository.countByStatus(EmailStatus.OPENED);
        long bounced = emailQueueRepository.countByStatus(EmailStatus.BOUNCED);
        long failed = emailQueueRepository.countByStatus(EmailStatus.FAILED);
        long attemptedTotal = delivered + bounced + failed;
        double deliveryRate = attemptedTotal == 0 ? 100.0 : (delivered * 100.0) / attemptedTotal;
        long scheduledUpcoming = emailQueueRepository.countByStatus(EmailStatus.PENDING);

        List<EmailStatus> sentStatuses = List.of(EmailStatus.SENT, EmailStatus.DELIVERED, EmailStatus.OPENED);
        Map<String, Long> sentByTemplate = new HashMap<>();
        for (var template : templateRepository.findAll()) {
            long count = emailQueueRepository.countByTemplateIdAndStatusIn(template.getId(), sentStatuses);
            if (count > 0) {
                sentByTemplate.put(template.getCategory().name(), count);
            }
        }
        return EmailDashboardStatsResponse.builder()
                .sentToday(sentToday)
                .deliveryRatePercent(Math.round(deliveryRate * 10) / 10.0)
                .failedCount(failed)
                .scheduledUpcomingCount(scheduledUpcoming)
                .sentByTemplateCategory(sentByTemplate)
                .build();
    }

    private void resetForRetry(EmailQueue emailQueue) {
        emailQueue.setStatus(EmailStatus.PENDING);
        emailQueue.setAttempts(0);
        emailQueue.setNextRetryAt(LocalDateTime.now());
        emailQueue.setFailureReason(null);
        emailQueue.setUpdatedAt(LocalDateTime.now());
    }

    private EmailQueueItemResponse toItemResponseWithTemplate(EmailQueue emailQueue) {
        EmailQueueItemResponse response = emailQueueMapper.toItemResponse(emailQueue);
        if (emailQueue.getTemplateId() != null) {
            templateRepository.findById(emailQueue.getTemplateId())
                    .ifPresent(t -> response.setTemplateCategory(t.getCategory()));
        }
        return response;
    }

    private EmailQueue findEmail(UUID id) {
        return emailQueueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EMAIL_QUEUE_ITEM_NOT_FOUND, "Email queue item not found: " + id));
    }
}
