package com.nexus.notification.application;

import com.nexus.notification.application.dto.NotificationResponse;
import com.nexus.notification.infrastructure.persistence.NotificationEntity;
import com.nexus.notification.infrastructure.persistence.NotificationRepository;
import com.nexus.tenant.infrastructure.persistence.TenantEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing in-app notifications.
 */
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Create a notification for a specific user.
     */
    public void notify(TenantEntity tenant, UUID userId, String type,
                       String title, String message, UUID referenceId) {
        NotificationEntity notification = new NotificationEntity(
                tenant, userId, type, title, message, referenceId);
        notificationRepository.save(notification);
    }

    /**
     * Get all notifications for the current user.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get unread count.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * Mark a single notification as read.
     */
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.markAsRead();
            notificationRepository.save(n);
        });
    }

    /**
     * Mark all notifications as read for a user.
     */
    public void markAllAsRead(UUID userId) {
        notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .forEach(n -> {
                    n.markAsRead();
                    notificationRepository.save(n);
                });
    }

    private NotificationResponse toResponse(NotificationEntity n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getReferenceId(), n.isRead(), n.getCreatedAt());
    }
}
