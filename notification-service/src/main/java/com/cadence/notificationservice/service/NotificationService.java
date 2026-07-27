package com.cadence.notificationservice.service;

import com.cadence.notificationservice.constants.NotificationStatus;
import com.cadence.notificationservice.dto.response.NotificationResponse;
import com.cadence.notificationservice.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Personal in-app notification CRUD, generic across any recipient
 * role. Built per the text spec's explicit "In-App Notifications"
 * requirement (Read/Unread/Archive/Delete/Mark All Read across
 * Recruiter/Candidate/HR/Hiring Manager/Technical Interviewer) --
 * the Figma's own recruiter-side page is actually an admin email
 * console, not a personal inbox (see README), so this generic API is
 * necessary supporting infrastructure for what the candidate's
 * (currently static) notification list is a real instance of.
 */
public interface NotificationService {

    PagedResponse<NotificationResponse> listNotifications(UUID recipientId, NotificationStatus status, Pageable pageable);

    NotificationResponse getNotification(UUID recipientId, UUID id);

    void markRead(UUID recipientId, UUID id);

    void markAllRead(UUID recipientId);

    void archive(UUID recipientId, UUID id);

    void delete(UUID recipientId, UUID id);

    long getUnreadCount(UUID recipientId);
}
