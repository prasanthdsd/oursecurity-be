package com.oursecurity.alert;

import com.oursecurity.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Emergency alerts", description = "SOS alerts that notify all guards and committee members")
public class AlertController {

    private final AlertService service;
    private final CurrentUser currentUser;

    public AlertController(AlertService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Raise an emergency alert (any role). All guards and committee members are notified")
    public AlertDto raise(@Valid @RequestBody RaiseAlertRequest req) {
        return service.raise(req, currentUser.get());
    }

    @GetMapping
    @Operation(summary = "List alerts, newest first. Residents see alerts they raised; guards and committee see all")
    public List<AlertDto> list(@RequestParam(required = false) AlertStatus status) {
        return service.list(status, currentUser.get());
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('GUARD','COMMITTEE')")
    @Operation(summary = "Guard or committee member says they are responding")
    public AlertDto acknowledge(@PathVariable Long id) {
        return service.acknowledge(id, currentUser.get());
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('GUARD','COMMITTEE')")
    @Operation(summary = "Guard or committee member closes the alert")
    public AlertDto resolve(@PathVariable Long id) {
        return service.resolve(id, currentUser.get());
    }
}
