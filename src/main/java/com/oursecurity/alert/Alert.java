package com.oursecurity.alert;

import com.oursecurity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "emergency_alert", indexes = @Index(columnList = "status"))
@Getter
@Setter
@NoArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType type;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status = AlertStatus.ACTIVE;

    @ManyToOne(optional = false)
    private User raisedBy;

    /** Flat of the person who raised it, if they are a resident. */
    private String flatNumber;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @ManyToOne
    private User acknowledgedBy;

    private Instant acknowledgedAt;

    @ManyToOne
    private User resolvedBy;

    private Instant resolvedAt;
}
