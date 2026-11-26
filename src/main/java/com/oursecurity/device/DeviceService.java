package com.oursecurity.device;

import com.oursecurity.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;

@Service
public class DeviceService {

    private final DeviceTokenRepository devices;

    public DeviceService(DeviceTokenRepository devices) {
        this.devices = devices;
    }

    /** Saves the token for this user. A token registered by someone else (a shared phone) moves to this user. */
    @Transactional
    public void register(String token, Platform platform, User user) {
        DeviceToken device = devices.findByToken(token).orElseGet(DeviceToken::new);
        device.setToken(token);
        device.setPlatform(platform);
        device.setUser(user);
        device.setUpdatedAt(Instant.now());
        devices.save(device);
    }

    /** Removes the token if it belongs to this user. Unknown tokens are ignored so logout never fails. */
    @Transactional
    public void unregister(String token, User user) {
        devices.findByToken(token)
                .filter(d -> d.getUser().getId().equals(user.getId()))
                .ifPresent(devices::delete);
    }

    @Transactional
    public void removeTokens(Collection<String> tokens) {
        if (!tokens.isEmpty()) {
            devices.deleteByTokens(tokens);
        }
    }
}
