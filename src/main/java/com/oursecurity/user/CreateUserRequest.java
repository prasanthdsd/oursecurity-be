package com.oursecurity.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "\\+?[0-9]{10,15}", message = "must be 10 to 15 digits") String phone,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotNull Role role,
        @Size(max = 20) String flatNumber) {}
