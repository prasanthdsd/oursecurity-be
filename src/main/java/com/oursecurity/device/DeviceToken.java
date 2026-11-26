package com.oursecurity.device;

import com.oursecurity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** An Expo push token ("ExponentPushToken[...]") for one phone, linked to the user signed in on it. */
@Entity
@Table(name = "device_token")
@Getter
@Setter
@NoArgsConstructor
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @ManyToOne(optional = false)
    private User user;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
