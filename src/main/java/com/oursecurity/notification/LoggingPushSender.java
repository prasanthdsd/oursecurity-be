package com.oursecurity.notification;

import com.oursecurity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushSender.class);

    @Override
    public void send(User recipient, Notification notification) {
        log.info("PUSH to user {} ({}): [{}] {}", recipient.getId(), recipient.getRole(),
                notification.getType(), notification.getTitle());
    }
}
