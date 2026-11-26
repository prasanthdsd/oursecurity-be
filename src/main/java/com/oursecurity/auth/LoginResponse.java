package com.oursecurity.auth;

import com.oursecurity.user.UserDto;

public record LoginResponse(String token, UserDto user) {}
