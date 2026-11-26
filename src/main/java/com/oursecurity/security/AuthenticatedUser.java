package com.oursecurity.security;

import com.oursecurity.user.Role;

/** The security principal for a request authenticated with a JWT. */
public record AuthenticatedUser(Long id, Role role) {}
