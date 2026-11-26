package com.oursecurity.device;

import com.oursecurity.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
@Tag(name = "Devices", description = "Expo push tokens for the signed-in user's phones")
public class DeviceController {

    public record RegisterDeviceRequest(@NotBlank @Size(max = 255) String token, @NotNull Platform platform) {}

    public record UnregisterDeviceRequest(@NotBlank @Size(max = 255) String token) {}

    private final DeviceService service;
    private final CurrentUser currentUser;

    public DeviceController(DeviceService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Register this phone's Expo push token. Call after login and whenever the token changes")
    public void register(@Valid @RequestBody RegisterDeviceRequest req) {
        service.register(req.token().trim(), req.platform(), currentUser.get());
    }

    @PostMapping("/unregister")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Stop pushes to this phone. Call on logout, before dropping the JWT")
    public void unregister(@Valid @RequestBody UnregisterDeviceRequest req) {
        service.unregister(req.token().trim(), currentUser.get());
    }
}
