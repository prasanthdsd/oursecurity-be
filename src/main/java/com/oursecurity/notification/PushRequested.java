package com.oursecurity.notification;

import java.util.Collection;

/** Published when notifications are saved; pushes go out after the transaction commits. */
public record PushRequested(Collection<Long> userIds, NotificationType type, String title, String body, Long referenceId) {}
