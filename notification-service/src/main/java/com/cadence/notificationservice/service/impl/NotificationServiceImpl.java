package com.cadence.notificationservice.service.impl;

import com.cadence.notificationservice.constants.NotificationStatus;
import com.cadence.notificationservice.dto.response.NotificationResponse;
import com.cadence.notificationservice.dto.response.PagedResponse;
import com.cadence.notificationservice.entity.Notification;
import com.cadence.notificationservice.exception.AccessDeniedApiException;
import com.cadence.notificationservice.exception.ErrorCode;
import com.cadence.notificationservice.exception.ResourceNotFoundException;
import com.cadence.notificationservice.mapper.NotificationMapper;
import com.cadence.notificationservice.repository.NotificationRepository;
import com.cadence.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public PagedResponse<NotificationResponse> listNotifications(UUID recipientId, NotificationStatus status, Pageable pageable) {
        Page<Notification> page = status != null
                ? notificationRepository.findAllByRecipientIdAndStatus(recipientId, status, pageable)
                : notificationRepository.findAllByRecipientIdAndStatusNot(recipientId, NotificationStatus.ARCHIVED, pageable);
        return PagedResponse.from(page.map(notificationMapper::toResponse));
    }

    @Override
    public NotificationResponse getNotification(UUID recipientId, UUID id) {
        return notificationMapper.toResponse(findOwned(recipientId, id));
    }

    @Override
    @Transactional
    public void markRead(UUID recipientId, UUID id) {
        Notification notification = findOwned(recipientId, id);
        if (notification.getStatus() == NotificationStatus.UNREAD) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(LocalDateTime.now());
            notification.setUpdatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllRead(UUID recipientId) {
        notificationRepository.markAllRead(recipientId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void archive(UUID recipientId, UUID id) {
        Notification notification = findOwned(recipientId, id);
        notification.setStatus(NotificationStatus.ARCHIVED);
        notification.setArchivedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void delete(UUID recipientId, UUID id) {
        Notification notification = findOwned(recipientId, id);
        notificationRepository.delete(notification);
    }

    @Override
    public long getUnreadCount(UUID recipientId) {
        return notificationRepository.countByRecipientIdAndStatus(recipientId, NotificationStatus.UNREAD);
    }

    private Notification findOwned(UUID recipientId, UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND, "Notification not found: " + id));
        if (!notification.getRecipientId().equals(recipientId)) {
            throw new AccessDeniedApiException("This notification does not belong to you");
        }
        return notification;
    }
}
