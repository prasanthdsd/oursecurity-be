package com.oursecurity.visitor;

import com.oursecurity.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/visits")
@Tag(name = "Visits", description = "Visitor pre-approval, walk-in approval and gate check-in/out")
public class VisitController {

    private final VisitService service;
    private final CurrentUser currentUser;

    public VisitController(VisitService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping("/preapprove")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Resident invites a visitor. Returns a 6-digit passCode to share with them")
    public VisitDto preApprove(@Valid @RequestBody VisitRequests.PreApprove req) {
        return service.preApprove(req, currentUser.get());
    }

    @PostMapping("/walk-in")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GUARD')")
    @Operation(summary = "Guard logs an unannounced visitor. Residents of the flat are asked to approve")
    public VisitDto walkIn(@Valid @RequestBody VisitRequests.WalkIn req) {
        return service.walkIn(req, currentUser.get());
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('GUARD')")
    @Operation(summary = "Guard enters a visitor's pass code. A valid code checks the visitor in")
    public VisitDto verify(@Valid @RequestBody VisitRequests.Verify req) {
        return service.verify(req.passCode(), currentUser.get());
    }

    @GetMapping
    @Operation(summary = "List visits, newest first. Residents see their own flat; guards and committee see all")
    public List<VisitDto> list(@RequestParam(required = false) VisitStatus status) {
        return service.list(status, currentUser.get());
    }

    @GetMapping("/{id}")
    @Operation(summary = "One visit")
    public VisitDto get(@PathVariable Long id) {
        return service.get(id, currentUser.get());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Resident approves a walk-in visitor waiting at the gate")
    public VisitDto approve(@PathVariable Long id) {
        return service.approve(id, currentUser.get());
    }

    @PostMapping("/{id}/deny")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Resident denies a walk-in visitor")
    public VisitDto deny(@PathVariable Long id) {
        return service.deny(id, currentUser.get());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Resident cancels a visit that has not checked in yet")
    public VisitDto cancel(@PathVariable Long id) {
        return service.cancel(id, currentUser.get());
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasRole('GUARD')")
    @Operation(summary = "Guard checks in a pre-approved or approved visitor by id")
    public VisitDto checkIn(@PathVariable Long id) {
        return service.checkIn(id, currentUser.get());
    }

    @PostMapping("/{id}/check-out")
    @PreAuthorize("hasRole('GUARD')")
    @Operation(summary = "Guard checks a visitor out as they leave")
    public VisitDto checkOut(@PathVariable Long id) {
        return service.checkOut(id, currentUser.get());
    }
}
