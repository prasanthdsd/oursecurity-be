package com.oursecurity.delivery;

import com.oursecurity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "delivery", indexes = {@Index(columnList = "flatNumber"), @Index(columnList = "status")})
@Getter
@Setter
@NoArgsConstructor
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String flatNumber;

    /** Courier or company, e.g. "Amazon", "Swiggy". */
    @Column(nullable = false)
    private String courier;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status = DeliveryStatus.AT_GATE;

    @Enumerated(EnumType.STRING)
    private DeliveryInstruction instruction;

    @Column(nullable = false)
    private Instant receivedAt = Instant.now();

    private Instant collectedAt;

    @ManyToOne(optional = false)
    private User loggedBy;
}
