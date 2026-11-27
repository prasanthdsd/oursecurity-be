package com.oursecurity.notification;

import java.util.List;
import java.util.Set;

/** Sends pushes to phones. */
public interface PushGateway {

    /** Sends the messages and returns the tokens the provider says are no longer registered. */
    Set<String> send(List<PushMessage> messages);
}
