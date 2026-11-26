package com.oursecurity.visitor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class VisitRequests {

    private VisitRequests() {}

    public record PreApprove(
            @NotBlank @Size(max = 100) String visitorName,
            @Size(max = 20) String visitorPhone,
            @Size(max = 200) String purpose,
            Instant expectedAt) {}

    public record WalkIn(
            @NotBlank @Size(max = 100) String visitorName,
            @Size(max = 20) String visitorPhone,
            @Size(max = 200) String purpose,
            @NotBlank @Size(max = 20) String flatNumber) {}

    public record Verify(@NotBlank @Pattern(regexp = "[0-9]{6}", message = "must be 6 digits") String passCode) {}
}
