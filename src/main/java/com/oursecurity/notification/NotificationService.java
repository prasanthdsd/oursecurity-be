package com.oursecurity.notification;

import com.oursecurity.common.ApiException;
import com.oursecurity.user.Role;
import com.oursecurity.user.User;
import com.oursecurity.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final UserRepository users;
    private final ApplicationEventPublisher events;

    public NotificationService(NotificationRepository notifications, UserRepository users,
                               ApplicationEventPublisher events) {
        this.notifications = notifications;
        this.users = users;
        this.events = events;
    }

    /** Notifies every active resident of a flat. */
    public void notifyFlat(String flatNumber, NotificationType type, String title, String body, Long refId) {
        notifyAll(users.findByRoleAndFlatNumberIgnoreCaseAndActiveTrue(Role.RESIDENT, flatNumber), type, title, body, refId);
    }

    /** Notifies every active user except {@code except}. */
    public void notifyEveryone(User except, NotificationType type, String title, String body, Long refId) {
        notifyAll(users.findByActiveTrueOrderByNameAsc().stream().filter(u -> !u.getId().equals(except.getId())).toList(),
                type, title, body, refId);
    }

    /** Notifies every active user with one of the given roles. */
    public void notifyRoles(Collection<Role> roles, NotificationType type, String title, String body, Long refId) {
        notifyAll(users.findByRoleInAndActiveTrue(roles), type, title, body, refId);
    }

    public void notifyAll(Collection<User> recipients, NotificationType type, String title, String body, Long refId) {
        for (User recipient : recipients) {
            Notification n = new Notification();
            n.setRecipient(recipient);
            n.setType(type);
            n.setTitle(title);
            n.setBody(body);
            n.setReferenceId(refId);
            notifications.save(n);
        }
        if (!recipients.isEmpty()) {
            events.publishEvent(new PushRequested(recipients.stream().map(User::getId).toList(), type, title, body, refId));
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> forUser(User user) {
        return notifications.findTop100ByRecipientIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(NotificationDto::from).toList();
    }

    @Transactional
    public NotificationDto markRead(Long id, User user) {
        Notification n = notifications.findById(id)
                .filter(x -> x.getRecipient().getId().equals(user.getId()))
                .orElseThrow(() -> ApiException.notFound("Notification"));
        n.setRead(true);
        return NotificationDto.from(n);
    }
}
