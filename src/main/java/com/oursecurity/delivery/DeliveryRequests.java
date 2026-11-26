package com.oursecurity.delivery;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class DeliveryRequests {

    private DeliveryRequests() {}

    public record Log(
            @NotBlank @Size(max = 20) String flatNumber,
            @NotBlank @Size(max = 100) String courier,
            @Size(max = 300) String description) {}

    public record Instruct(@NotNull DeliveryInstruction instruction) {}
}
