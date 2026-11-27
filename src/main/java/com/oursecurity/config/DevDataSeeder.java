package com.oursecurity.config;

import com.oursecurity.notice.Notice;
import com.oursecurity.notice.NoticeRepository;
import com.oursecurity.user.Role;
import com.oursecurity.user.User;
import com.oursecurity.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates demo accounts and a welcome notice on an empty database. Turned on by app.seed.enabled (on by default, off in prod). */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository users;
    private final NoticeRepository notices;
    private final PasswordEncoder encoder;

    public DevDataSeeder(UserRepository users, NoticeRepository notices, PasswordEncoder encoder) {
        this.users = users;
        this.notices = notices;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        User admin = users.save(new User("Committee Admin", "9000000001", encoder.encode("admin123"), Role.COMMITTEE, null));
        users.save(new User("Gate Guard", "9000000002", encoder.encode("guard123"), Role.GUARD, null));
        users.save(new User("Asha Resident", "9000000003", encoder.encode("resident123"), Role.RESIDENT, "A-101"));
        users.save(new User("Ravi Resident", "9000000004", encoder.encode("resident123"), Role.RESIDENT, "B-202"));
        Notice welcome = new Notice();
        welcome.setTitle("Welcome to OurSecurity");
        welcome.setBody("Pre-approve your guests from the app and share the pass code. Use the SOS button in an emergency.");
        welcome.setPostedBy(admin);
        notices.save(welcome);
        log.warn("Seeded demo users (committee 9000000001/admin123, guard 9000000002/guard123, "
                + "residents 9000000003 and 9000000004/resident123). Do not use in production.");
    }
}
