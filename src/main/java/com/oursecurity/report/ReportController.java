package com.oursecurity.report;

import com.oursecurity.alert.AlertRepository;
import com.oursecurity.alert.AlertStatus;
import com.oursecurity.config.AppProperties;
import com.oursecurity.delivery.DeliveryRepository;
import com.oursecurity.delivery.DeliveryStatus;
import com.oursecurity.user.Role;
import com.oursecurity.user.UserRepository;
import com.oursecurity.visitor.VisitRepository;
import com.oursecurity.visitor.VisitStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports")
public class ReportController {

    private final VisitRepository visits;
    private final DeliveryRepository deliveries;
    private final AlertRepository alerts;
    private final UserRepository users;
    private final ZoneId zone;

    public ReportController(VisitRepository visits, DeliveryRepository deliveries, AlertRepository alerts,
                            UserRepository users, AppProperties props) {
        this.visits = visits;
        this.deliveries = deliveries;
        this.alerts = alerts;
        this.users = users;
        this.zone = ZoneId.of(props.timeZone());
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('COMMITTEE')")
    @Operation(summary = "Dashboard counts. visitorsToday counts check-ins since midnight in the community's time zone; "
            + "activeAlerts counts alerts not yet resolved")
    public SummaryDto summary() {
        var startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        return new SummaryDto(
                visits.countByCheckedInAtGreaterThanEqual(startOfToday),
                visits.countByStatus(VisitStatus.PENDING_APPROVAL),
                deliveries.countByStatus(DeliveryStatus.AT_GATE),
                alerts.countByStatusIn(EnumSet.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED)),
                users.countByRoleAndActiveTrue(Role.RESIDENT),
                users.countByRoleAndActiveTrue(Role.GUARD));
    }
}
