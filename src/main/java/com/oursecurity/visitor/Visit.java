package com.oursecurity.visitor;

import com.oursecurity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "visit", indexes = {
        @Index(columnList = "flatNumber"),
        @Index(columnList = "status"),
        @Index(columnList = "passCode")
})
@Getter
@Setter
@NoArgsConstructor
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String visitorName;

    private String visitorPhone;

    private String purpose;

    @Column(nullable = false)
    private String flatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisitStatus status;

    /** 6-digit code for pre-approved visitors; null for walk-ins. */
    @Column(length = 6)
    private String passCode;

    private Instant expectedAt;

    /** A pre-approved pass stops working after this time. */
    private Instant validUntil;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant checkedInAt;

    private Instant checkedOutAt;

    @ManyToOne(optional = false)
    private User createdBy;

    /** Resident who approved or denied a walk-in. */
    @ManyToOne
    private User respondedBy;

    @ManyToOne
    private User checkedInBy;
}
