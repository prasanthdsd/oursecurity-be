package com.oursecurity.user;

public record UserDto(Long id, String name, String phone, Role role, String flatNumber) {

    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getName(), u.getPhone(), u.getRole(), u.getFlatNumber());
    }
}
