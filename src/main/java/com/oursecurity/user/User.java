package com.oursecurity.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Login identifier. Stored as typed, e.g. "9876543210". */
    @Column(nullable = false, unique = true)
    private String phone;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Flat or unit, e.g. "A-101". Required for residents, empty for guards. */
    private String flatNumber;

    /** Removed users are deactivated rather than deleted, so their history stays intact. */
    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public User(String name, String phone, String passwordHash, Role role, String flatNumber) {
        this.name = name;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.flatNumber = flatNumber;
    }
}
