package com.oursecurity.delivery;

import com.oursecurity.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
@Tag(name = "Deliveries", description = "Packages received at the gate")
public class DeliveryController {

    private final DeliveryService service;
    private final CurrentUser currentUser;

    public DeliveryController(DeliveryService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GUARD')")
    @Operation(summary = "Guard logs a delivery for a flat. Residents of the flat are notified")
    public DeliveryDto log(@Valid @RequestBody DeliveryRequests.Log req) {
        return service.log(req, currentUser.get());
    }

    @GetMapping
    @Operation(summary = "List deliveries, newest first. Residents see their own flat; guards and committee see all")
    public List<DeliveryDto> list(@RequestParam(required = false) DeliveryStatus status) {
        return service.list(status, currentUser.get());
    }

    @PostMapping("/{id}/instruction")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Resident says whether to leave the package at the gate or send it up")
    public DeliveryDto instruct(@PathVariable Long id, @Valid @RequestBody DeliveryRequests.Instruct req) {
        return service.instruct(id, req.instruction(), currentUser.get());
    }

    @PostMapping("/{id}/collect")
    @PreAuthorize("hasRole('GUARD')")
    @Operation(summary = "Guard marks the package as collected by or delivered to the resident")
    public DeliveryDto collect(@PathVariable Long id) {
        return service.collect(id);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('GUARD')")
    @Operation(summary = "Guard marks the package as returned to the courier")
    public DeliveryDto markReturned(@PathVariable Long id) {
        return service.markReturned(id);
    }
}
