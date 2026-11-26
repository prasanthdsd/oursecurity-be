package com.oursecurity.alert;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RaiseAlertRequest(@NotNull AlertType type, @Size(max = 500) String message) {}
