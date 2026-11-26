package com.oursecurity.notification;

import com.oursecurity.user.User;

/**
 * Delivers a push notification to a user's phone. The default implementation only logs;
 * swap in an FCM or Expo push implementation when device tokens are collected.
 */
public interface PushSender {

    void send(User recipient, Notification notification);
}
