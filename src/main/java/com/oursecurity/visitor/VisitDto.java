package com.oursecurity.visitor;

import java.time.Instant;

public record VisitDto(Long id, String visitorName, String visitorPhone, String purpose, String flatNumber,
                       VisitStatus status, String passCode, Instant expectedAt, Instant validUntil,
                       Instant createdAt, Instant checkedInAt, Instant checkedOutAt, String createdByName) {

    public static VisitDto from(Visit v) {
        return new VisitDto(v.getId(), v.getVisitorName(), v.getVisitorPhone(), v.getPurpose(), v.getFlatNumber(),
                v.getStatus(), v.getPassCode(), v.getExpectedAt(), v.getValidUntil(), v.getCreatedAt(),
                v.getCheckedInAt(), v.getCheckedOutAt(), v.getCreatedBy().getName());
    }
}
