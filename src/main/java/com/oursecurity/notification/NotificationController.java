package com.oursecurity.notification;

import com.oursecurity.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "In-app notifications for the logged-in user")
public class NotificationController {

    private final NotificationService service;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "My latest 100 notifications, newest first")
    public List<NotificationDto> list() {
        return service.forUser(currentUser.get());
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark one of my notifications as read")
    public NotificationDto markRead(@PathVariable Long id) {
        return service.markRead(id, currentUser.get());
    }
}
