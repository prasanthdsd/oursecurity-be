package com.oursecurity.user;

import com.oursecurity.common.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserDto> list(Role role) {
        List<User> found = role == null
                ? users.findByActiveTrueOrderByNameAsc()
                : users.findByRoleAndActiveTrueOrderByNameAsc(role);
        return found.stream().map(UserDto::from).toList();
    }

    @Transactional
    public UserDto create(CreateUserRequest req) {
        String phone = req.phone().trim();
        if (users.existsByPhone(phone)) {
            throw ApiException.conflict("A user with this phone number already exists");
        }
        String flat = req.flatNumber() == null || req.flatNumber().isBlank() ? null : req.flatNumber().trim().toUpperCase();
        if (req.role() == Role.RESIDENT && flat == null) {
            throw ApiException.badRequest("flatNumber is required for residents");
        }
        User user = new User(req.name().trim(), phone, passwordEncoder.encode(req.password()), req.role(), flat);
        return UserDto.from(users.save(user));
    }

    @Transactional
    public void deactivate(Long id, User actor) {
        User user = users.findById(id).filter(User::isActive).orElseThrow(() -> ApiException.notFound("User"));
        if (user.getId().equals(actor.getId())) {
            throw ApiException.badRequest("You cannot remove your own account");
        }
        user.setActive(false);
    }
}
