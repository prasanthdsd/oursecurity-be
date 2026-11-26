package com.oursecurity.auth;

import com.oursecurity.security.CurrentUser;
import com.oursecurity.security.JwtService;
import com.oursecurity.user.User;
import com.oursecurity.user.UserDto;
import com.oursecurity.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUser currentUser;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                          CurrentUser currentUser) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Log in with phone and password. Returns a JWT and the user")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        User user = users.findByPhone(req.phone().trim())
                .filter(User::isActive)
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("bad credentials"));
        return new LoginResponse(jwtService.issue(user), UserDto.from(user));
    }

    @GetMapping("/me")
    @Operation(summary = "The logged-in user")
    public UserDto me() {
        return UserDto.from(currentUser.get());
    }
}
