package com.oursecurity.notification;

import java.util.Map;

/** One push to one device. {@code data} is {type, id} so the app can open the right screen. */
public record PushMessage(String to, String title, String body, Map<String, Object> data, String channelId) {}
