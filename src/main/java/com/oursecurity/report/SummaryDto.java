package com.oursecurity.report;

public record SummaryDto(long visitorsToday, long pendingApprovals, long deliveriesAtGate, long activeAlerts,
                         long residents, long guards) {}
