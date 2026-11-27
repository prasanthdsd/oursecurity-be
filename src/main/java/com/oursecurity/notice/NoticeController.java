package com.oursecurity.notice;

import com.oursecurity.notification.NotificationService;
import com.oursecurity.notification.NotificationType;
import com.oursecurity.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@Tag(name = "Notices", description = "Community notice board")
public class NoticeController {

    public record CreateNoticeRequest(@NotBlank @Size(max = 200) String title, @NotBlank @Size(max = 4000) String body) {}

    private final NoticeRepository notices;
    private final CurrentUser currentUser;
    private final NotificationService notifications;

    public NoticeController(NoticeRepository notices, CurrentUser currentUser, NotificationService notifications) {
        this.notices = notices;
        this.currentUser = currentUser;
        this.notifications = notifications;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Latest 50 notices, newest first")
    public List<NoticeDto> list() {
        return notices.findTop50ByOrderByCreatedAtDesc().stream().map(NoticeDto::from).toList();
    }

    @PostMapping
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COMMITTEE')")
    @Operation(summary = "Committee posts a notice")
    public NoticeDto create(@Valid @RequestBody CreateNoticeRequest req) {
        Notice n = new Notice();
        n.setTitle(req.title().trim());
        n.setBody(req.body().trim());
        n.setPostedBy(currentUser.get());
        notices.save(n);
        notifications.notifyEveryone(n.getPostedBy(), NotificationType.NOTICE, "Notice: " + n.getTitle(),
                n.getBody().length() > 140 ? n.getBody().substring(0, 140) + "…" : n.getBody(), n.getId());
        return NoticeDto.from(n);
    }
}
