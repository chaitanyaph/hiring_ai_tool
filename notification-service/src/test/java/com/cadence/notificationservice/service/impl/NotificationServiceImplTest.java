package com.cadence.notificationservice.service.impl;

import com.cadence.notificationservice.constants.ColorTone;
import com.cadence.notificationservice.constants.NotificationCategory;
import com.cadence.notificationservice.constants.NotificationStatus;
import com.cadence.notificationservice.entity.Notification;
import com.cadence.notificationservice.exception.AccessDeniedApiException;
import com.cadence.notificationservice.exception.ResourceNotFoundException;
import com.cadence.notificationservice.mapper.NotificationMapper;
import com.cadence.notificationservice.mapper.NotificationMapperImpl;
import com.cadence.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;

    private final NotificationMapper notificationMapper = new NotificationMapperImpl();

    private NotificationServiceImpl notificationService;

    private UUID recipientId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationRepository, notificationMapper);
        recipientId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
        lenient().when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Notification notification(UUID owner, NotificationStatus status) {
        return Notification.builder().id(notificationId).recipientId(owner).recipientRole("CANDIDATE")
                .category(NotificationCategory.APPLICATION).title("t").message("m").colorTone(ColorTone.INFO)
                .status(status).build();
    }

    @Test
    void markRead_shouldTransitionUnreadToReadAndSetTimestamp() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification(recipientId, NotificationStatus.UNREAD)));

        notificationService.markRead(recipientId, notificationId);

        verify(notificationRepository).save(argThat(n -> n.getStatus() == NotificationStatus.READ && n.getReadAt() != null));
    }

    @Test
    void markRead_shouldThrow_whenNotOwnedByCaller() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification(UUID.randomUUID(), NotificationStatus.UNREAD)));

        assertThatThrownBy(() -> notificationService.markRead(recipientId, notificationId))
                .isInstanceOf(AccessDeniedApiException.class);
    }

    @Test
    void getNotification_shouldThrow_whenNotFound() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getNotification(recipientId, notificationId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void archive_shouldSetArchivedStatusAndTimestamp() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification(recipientId, NotificationStatus.READ)));

        notificationService.archive(recipientId, notificationId);

        verify(notificationRepository).save(argThat(n -> n.getStatus() == NotificationStatus.ARCHIVED && n.getArchivedAt() != null));
    }

    @Test
    void getUnreadCount_shouldDelegateToRepository() {
        when(notificationRepository.countByRecipientIdAndStatus(recipientId, NotificationStatus.UNREAD)).thenReturn(7L);

        assertThat(notificationService.getUnreadCount(recipientId)).isEqualTo(7L);
    }
}
