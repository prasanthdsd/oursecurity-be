package com.oursecurity.notification;

import com.oursecurity.device.DeviceService;
import com.oursecurity.device.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns saved notifications into pushes to every registered phone of the recipients.
 * Runs after the commit and off the request thread, so a slow push never delays the API response
 * and a rolled-back action never sends a push.
 */
@Component
public class PushDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PushDispatcher.class);

    private final DeviceTokenRepository devices;
    private final DeviceService deviceService;
    private final PushGateway gateway;

    public PushDispatcher(DeviceTokenRepository devices, DeviceService deviceService, PushGateway gateway) {
        this.devices = devices;
        this.deviceService = deviceService;
        this.gateway = gateway;
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void onPushRequested(PushRequested event) {
        try {
            List<String> tokens = event.userIds().isEmpty() ? List.of() : devices.findTokensForUsers(event.userIds());
            if (tokens.isEmpty()) {
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("type", pushType(event.type()));
            data.put("id", event.referenceId());
            String channel = channel(event.type());
            List<PushMessage> messages = tokens.stream()
                    .map(t -> new PushMessage(t, event.title(), event.body(), data, channel))
                    .toList();
            Set<String> dead = gateway.send(messages);
            if (!dead.isEmpty()) {
                log.info("Removing {} push tokens that are no longer registered", dead.size());
                deviceService.removeTokens(dead);
            }
        } catch (RuntimeException e) {
            log.warn("Sending push for {} failed", event.type(), e);
        }
    }

    /** The {@code data.type} value the mobile app switches on. */
    static String pushType(NotificationType type) {
        return switch (type) {
            case VISITOR_APPROVAL_REQUEST -> "VISIT_REQUEST";
            case VISITOR_APPROVED, VISITOR_DENIED, VISITOR_ARRIVED -> "VISIT_UPDATE";
            case DELIVERY_AT_GATE, DELIVERY_INSTRUCTION -> "DELIVERY";
            case EMERGENCY_ALERT, ALERT_UPDATE -> "ALERT";
            case NOTICE -> "NOTICE";
        };
    }

    /** Android notification channel; the app creates gate, alerts and default. */
    static String channel(NotificationType type) {
        return switch (type) {
            case VISITOR_APPROVAL_REQUEST, VISITOR_APPROVED, VISITOR_DENIED, VISITOR_ARRIVED -> "gate";
            case EMERGENCY_ALERT, ALERT_UPDATE -> "alerts";
            default -> "default";
        };
    }
}
