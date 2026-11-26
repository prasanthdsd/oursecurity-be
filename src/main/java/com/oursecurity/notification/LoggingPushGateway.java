package com.oursecurity.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/** Logs pushes instead of sending them. Used when app.push.provider=log. */
@Component
@ConditionalOnProperty(name = "app.push.provider", havingValue = "log")
public class LoggingPushGateway implements PushGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushGateway.class);

    @Override
    public Set<String> send(List<PushMessage> messages) {
        messages.forEach(m -> log.info("PUSH (not sent) to {} on {}: {} {}", m.to(), m.channelId(), m.title(), m.data()));
        return Set.of();
    }
}
