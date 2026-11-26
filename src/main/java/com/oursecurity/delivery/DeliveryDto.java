package com.oursecurity.delivery;

import java.time.Instant;

public record DeliveryDto(Long id, String flatNumber, String courier, String description, DeliveryStatus status,
                          DeliveryInstruction instruction, Instant receivedAt, Instant collectedAt,
                          String loggedByName) {

    public static DeliveryDto from(Delivery d) {
        return new DeliveryDto(d.getId(), d.getFlatNumber(), d.getCourier(), d.getDescription(), d.getStatus(),
                d.getInstruction(), d.getReceivedAt(), d.getCollectedAt(), d.getLoggedBy().getName());
    }
}
