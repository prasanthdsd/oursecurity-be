package com.oursecurity.alert;

import com.oursecurity.user.User;

import java.time.Instant;

public record AlertDto(Long id, AlertType type, String message, AlertStatus status, String raisedByName,
                       String raisedByPhone, String flatNumber, Instant createdAt, String acknowledgedByName,
                       Instant acknowledgedAt, String resolvedByName, Instant resolvedAt) {

    public static AlertDto from(Alert a) {
        return new AlertDto(a.getId(), a.getType(), a.getMessage(), a.getStatus(), a.getRaisedBy().getName(),
                a.getRaisedBy().getPhone(), a.getFlatNumber(), a.getCreatedAt(), name(a.getAcknowledgedBy()),
                a.getAcknowledgedAt(), name(a.getResolvedBy()), a.getResolvedAt());
    }

    private static String name(User u) {
        return u == null ? null : u.getName();
    }
}
