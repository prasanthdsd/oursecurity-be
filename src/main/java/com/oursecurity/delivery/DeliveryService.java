package com.oursecurity.delivery;

import com.oursecurity.common.ApiException;
import com.oursecurity.notification.NotificationService;
import com.oursecurity.notification.NotificationType;
import com.oursecurity.user.Role;
import com.oursecurity.user.User;
import com.oursecurity.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveries;
    private final UserRepository users;
    private final NotificationService notifications;

    public DeliveryService(DeliveryRepository deliveries, UserRepository users, NotificationService notifications) {
        this.deliveries = deliveries;
        this.users = users;
        this.notifications = notifications;
    }

    @Transactional
    public DeliveryDto log(DeliveryRequests.Log req, User guard) {
        String flat = req.flatNumber().trim().toUpperCase();
        if (users.findByRoleAndFlatNumberIgnoreCaseAndActiveTrue(Role.RESIDENT, flat).isEmpty()) {
            throw ApiException.badRequest("No residents are registered for flat " + flat);
        }
        Delivery d = new Delivery();
        d.setFlatNumber(flat);
        d.setCourier(req.courier().trim());
        d.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().trim());
        d.setLoggedBy(guard);
        deliveries.save(d);
        notifications.notifyFlat(flat, NotificationType.DELIVERY_AT_GATE,
                "Delivery from " + d.getCourier() + " at the gate",
                "Tell the guard to leave it at the gate or send it up", d.getId());
        return DeliveryDto.from(d);
    }

    @Transactional(readOnly = true)
    public List<DeliveryDto> list(DeliveryStatus status, User user) {
        List<Delivery> found;
        if (user.getRole() == Role.RESIDENT) {
            found = status == null
                    ? deliveries.findByFlatNumberIgnoreCaseOrderByReceivedAtDesc(user.getFlatNumber())
                    : deliveries.findByFlatNumberIgnoreCaseAndStatusOrderByReceivedAtDesc(user.getFlatNumber(), status);
        } else {
            found = status == null ? deliveries.findAllByOrderByReceivedAtDesc() : deliveries.findByStatusOrderByReceivedAtDesc(status);
        }
        return found.stream().map(DeliveryDto::from).toList();
    }

    @Transactional
    public DeliveryDto instruct(Long id, DeliveryInstruction instruction, User resident) {
        Delivery d = find(id);
        if (resident.getFlatNumber() == null || !resident.getFlatNumber().equalsIgnoreCase(d.getFlatNumber())) {
            throw ApiException.notFound("Delivery");
        }
        requireAtGate(d);
        d.setInstruction(instruction);
        notifications.notifyRoles(List.of(Role.GUARD), NotificationType.DELIVERY_INSTRUCTION,
                "Flat " + d.getFlatNumber() + ": " + (instruction == DeliveryInstruction.SEND_UP ? "send up" : "leave at gate"),
                d.getCourier() + " delivery", d.getId());
        return DeliveryDto.from(d);
    }

    @Transactional
    public DeliveryDto collect(Long id) {
        Delivery d = find(id);
        requireAtGate(d);
        d.setStatus(DeliveryStatus.COLLECTED);
        d.setCollectedAt(Instant.now());
        return DeliveryDto.from(d);
    }

    @Transactional
    public DeliveryDto markReturned(Long id) {
        Delivery d = find(id);
        requireAtGate(d);
        d.setStatus(DeliveryStatus.RETURNED);
        d.setCollectedAt(Instant.now());
        return DeliveryDto.from(d);
    }

    private Delivery find(Long id) {
        return deliveries.findById(id).orElseThrow(() -> ApiException.notFound("Delivery"));
    }

    private static void requireAtGate(Delivery d) {
        if (d.getStatus() != DeliveryStatus.AT_GATE) {
            throw ApiException.conflict("Delivery is " + d.getStatus());
        }
    }
}
