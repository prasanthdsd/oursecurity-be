package com.oursecurity.notification;

import java.time.Instant;

public record NotificationDto(Long id, NotificationType type, String title, String body, Long referenceId,
                              boolean read, Instant createdAt) {

    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getReferenceId(),
                n.isRead(), n.getCreatedAt());
    }
}
