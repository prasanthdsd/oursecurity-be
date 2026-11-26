package com.oursecurity.alert;

import com.oursecurity.common.ApiException;
import com.oursecurity.notification.NotificationService;
import com.oursecurity.notification.NotificationType;
import com.oursecurity.user.Role;
import com.oursecurity.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alerts;
    private final NotificationService notifications;

    public AlertService(AlertRepository alerts, NotificationService notifications) {
        this.alerts = alerts;
        this.notifications = notifications;
    }

    @Transactional
    public AlertDto raise(RaiseAlertRequest req, User raiser) {
        Alert a = new Alert();
        a.setType(req.type());
        a.setMessage(req.message() == null || req.message().isBlank() ? null : req.message().trim());
        a.setRaisedBy(raiser);
        a.setFlatNumber(raiser.getFlatNumber());
        alerts.save(a);
        String where = raiser.getFlatNumber() == null ? raiser.getName() : raiser.getFlatNumber();
        notifications.notifyRoles(List.of(Role.GUARD, Role.COMMITTEE), NotificationType.EMERGENCY_ALERT,
                "\uD83D\uDEA8 " + label(a.getType()) + ", " + where,
                (a.getMessage() == null ? "" : a.getMessage() + " · ") + "Call " + raiser.getName() + " on " + raiser.getPhone(),
                a.getId());
        return AlertDto.from(a);
    }

    @Transactional(readOnly = true)
    public List<AlertDto> list(AlertStatus status, User user) {
        List<Alert> found;
        if (user.getRole() == Role.RESIDENT) {
            found = status == null
                    ? alerts.findByRaisedByIdOrderByCreatedAtDesc(user.getId())
                    : alerts.findByRaisedByIdAndStatusOrderByCreatedAtDesc(user.getId(), status);
        } else {
            found = status == null ? alerts.findAllByOrderByCreatedAtDesc() : alerts.findByStatusOrderByCreatedAtDesc(status);
        }
        return found.stream().map(AlertDto::from).toList();
    }

    @Transactional
    public AlertDto acknowledge(Long id, User responder) {
        Alert a = find(id);
        if (a.getStatus() != AlertStatus.ACTIVE) {
            throw ApiException.conflict("Alert is " + a.getStatus());
        }
        a.setStatus(AlertStatus.ACKNOWLEDGED);
        a.setAcknowledgedBy(responder);
        a.setAcknowledgedAt(Instant.now());
        notifications.notifyAll(List.of(a.getRaisedBy()), NotificationType.ALERT_UPDATE,
                "Help is on the way", responder.getName() + " acknowledged your alert", a.getId());
        return AlertDto.from(a);
    }

    @Transactional
    public AlertDto resolve(Long id, User responder) {
        Alert a = find(id);
        if (a.getStatus() == AlertStatus.RESOLVED) {
            throw ApiException.conflict("Alert is already resolved");
        }
        if (a.getAcknowledgedBy() == null) {
            a.setAcknowledgedBy(responder);
            a.setAcknowledgedAt(Instant.now());
        }
        a.setStatus(AlertStatus.RESOLVED);
        a.setResolvedBy(responder);
        a.setResolvedAt(Instant.now());
        notifications.notifyAll(List.of(a.getRaisedBy()), NotificationType.ALERT_UPDATE,
                "Alert resolved", responder.getName() + " marked your alert as resolved", a.getId());
        return AlertDto.from(a);
    }

    private static String label(AlertType type) {
        return switch (type) {
            case MEDICAL -> "Medical emergency";
            case FIRE -> "Fire";
            case SECURITY -> "Security emergency";
            case OTHER -> "Emergency";
        };
    }

    private Alert find(Long id) {
        return alerts.findById(id).orElseThrow(() -> ApiException.notFound("Alert"));
    }
}
