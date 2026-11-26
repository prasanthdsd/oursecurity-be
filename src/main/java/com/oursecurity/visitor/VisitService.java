package com.oursecurity.visitor;

import com.oursecurity.common.ApiException;
import com.oursecurity.config.AppProperties;
import com.oursecurity.notification.NotificationService;
import com.oursecurity.notification.NotificationType;
import com.oursecurity.user.Role;
import com.oursecurity.user.User;
import com.oursecurity.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class VisitService {

    /** Statuses in which a pass code can still be used at the gate. */
    private static final Set<VisitStatus> USABLE = EnumSet.of(VisitStatus.PRE_APPROVED, VisitStatus.APPROVED);

    private final VisitRepository visits;
    private final UserRepository users;
    private final NotificationService notifications;
    private final Duration passValidity;
    private final SecureRandom random = new SecureRandom();

    public VisitService(VisitRepository visits, UserRepository users, NotificationService notifications,
                        AppProperties props) {
        this.visits = visits;
        this.users = users;
        this.notifications = notifications;
        this.passValidity = props.passValidity();
    }

    @Transactional
    public VisitDto preApprove(VisitRequests.PreApprove req, User resident) {
        requireFlat(resident);
        Visit v = new Visit();
        v.setVisitorName(req.visitorName().trim());
        v.setVisitorPhone(trimToNull(req.visitorPhone()));
        v.setPurpose(trimToNull(req.purpose()));
        v.setFlatNumber(resident.getFlatNumber());
        v.setStatus(VisitStatus.PRE_APPROVED);
        v.setExpectedAt(req.expectedAt());
        Instant from = req.expectedAt() != null && req.expectedAt().isAfter(Instant.now()) ? req.expectedAt() : Instant.now();
        v.setValidUntil(from.plus(passValidity));
        v.setPassCode(newPassCode());
        v.setCreatedBy(resident);
        return VisitDto.from(visits.save(v));
    }

    @Transactional
    public VisitDto walkIn(VisitRequests.WalkIn req, User guard) {
        String flat = req.flatNumber().trim().toUpperCase();
        if (users.findByRoleAndFlatNumberIgnoreCaseAndActiveTrue(Role.RESIDENT, flat).isEmpty()) {
            throw ApiException.badRequest("No residents are registered for flat " + flat);
        }
        Visit v = new Visit();
        v.setVisitorName(req.visitorName().trim());
        v.setVisitorPhone(trimToNull(req.visitorPhone()));
        v.setPurpose(trimToNull(req.purpose()));
        v.setFlatNumber(flat);
        v.setStatus(VisitStatus.PENDING_APPROVAL);
        v.setCreatedBy(guard);
        visits.save(v);
        notifications.notifyFlat(flat, NotificationType.VISITOR_APPROVAL_REQUEST,
                v.getVisitorName() + " is at the gate",
                v.getPurpose() == null ? "Approve or deny entry" : "Purpose: " + v.getPurpose(), v.getId());
        return VisitDto.from(v);
    }

    @Transactional
    public VisitDto verify(String passCode, User guard) {
        Visit v = visits.findFirstByPassCodeAndStatusIn(passCode, USABLE)
                .orElseThrow(() -> ApiException.notFound("Valid pass"));
        if (v.getValidUntil() != null && Instant.now().isAfter(v.getValidUntil())) {
            throw ApiException.badRequest("This pass has expired");
        }
        return VisitDto.from(checkIn(v, guard));
    }

    @Transactional(readOnly = true)
    public List<VisitDto> list(VisitStatus status, User user) {
        List<Visit> found;
        if (user.getRole() == Role.RESIDENT) {
            found = status == null
                    ? visits.findByFlatNumberIgnoreCaseOrderByCreatedAtDesc(user.getFlatNumber())
                    : visits.findByFlatNumberIgnoreCaseAndStatusOrderByCreatedAtDesc(user.getFlatNumber(), status);
        } else {
            found = status == null ? visits.findAllByOrderByCreatedAtDesc() : visits.findByStatusOrderByCreatedAtDesc(status);
        }
        return found.stream().map(VisitDto::from).toList();
    }

    @Transactional(readOnly = true)
    public VisitDto get(Long id, User user) {
        Visit v = find(id);
        if (user.getRole() == Role.RESIDENT) {
            requireOwnFlat(v, user);
        }
        return VisitDto.from(v);
    }

    @Transactional
    public VisitDto approve(Long id, User resident) {
        Visit v = find(id);
        requireOwnFlat(v, resident);
        requireStatus(v, VisitStatus.PENDING_APPROVAL);
        v.setStatus(VisitStatus.APPROVED);
        v.setRespondedBy(resident);
        notifications.notifyRoles(List.of(Role.GUARD), NotificationType.VISITOR_APPROVED,
                "Entry approved for " + v.getVisitorName(), "Flat " + v.getFlatNumber() + " approved", v.getId());
        return VisitDto.from(v);
    }

    @Transactional
    public VisitDto deny(Long id, User resident) {
        Visit v = find(id);
        requireOwnFlat(v, resident);
        requireStatus(v, VisitStatus.PENDING_APPROVAL);
        v.setStatus(VisitStatus.DENIED);
        v.setRespondedBy(resident);
        notifications.notifyRoles(List.of(Role.GUARD), NotificationType.VISITOR_DENIED,
                "Entry denied for " + v.getVisitorName(), "Flat " + v.getFlatNumber() + " denied", v.getId());
        return VisitDto.from(v);
    }

    @Transactional
    public VisitDto cancel(Long id, User resident) {
        Visit v = find(id);
        requireOwnFlat(v, resident);
        requireStatus(v, VisitStatus.PRE_APPROVED, VisitStatus.PENDING_APPROVAL, VisitStatus.APPROVED);
        v.setStatus(VisitStatus.CANCELLED);
        return VisitDto.from(v);
    }

    @Transactional
    public VisitDto checkIn(Long id, User guard) {
        Visit v = find(id);
        requireStatus(v, VisitStatus.PRE_APPROVED, VisitStatus.APPROVED);
        return VisitDto.from(checkIn(v, guard));
    }

    @Transactional
    public VisitDto checkOut(Long id, User guard) {
        Visit v = find(id);
        requireStatus(v, VisitStatus.CHECKED_IN);
        v.setStatus(VisitStatus.CHECKED_OUT);
        v.setCheckedOutAt(Instant.now());
        return VisitDto.from(v);
    }

    private Visit checkIn(Visit v, User guard) {
        v.setStatus(VisitStatus.CHECKED_IN);
        v.setCheckedInAt(Instant.now());
        v.setCheckedInBy(guard);
        notifications.notifyFlat(v.getFlatNumber(), NotificationType.VISITOR_ARRIVED,
                v.getVisitorName() + " has entered", "Checked in at the gate", v.getId());
        return v;
    }

    private Visit find(Long id) {
        return visits.findById(id).orElseThrow(() -> ApiException.notFound("Visit"));
    }

    private static void requireFlat(User resident) {
        if (resident.getFlatNumber() == null) {
            throw ApiException.badRequest("Your account has no flat number");
        }
    }

    private static void requireOwnFlat(Visit v, User resident) {
        if (resident.getFlatNumber() == null || !resident.getFlatNumber().equalsIgnoreCase(v.getFlatNumber())) {
            throw ApiException.notFound("Visit");
        }
    }

    private static void requireStatus(Visit v, VisitStatus... allowed) {
        for (VisitStatus s : allowed) {
            if (v.getStatus() == s) {
                return;
            }
        }
        throw ApiException.conflict("Visit is " + v.getStatus());
    }

    private String newPassCode() {
        for (int i = 0; i < 20; i++) {
            String code = String.format("%06d", random.nextInt(1_000_000));
            if (!visits.existsByPassCodeAndStatusIn(code, USABLE)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique pass code");
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
