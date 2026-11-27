package com.pollpulse.user;

import java.util.List;

public record UserResponse(
        Long id,
        String name,
        String email,
        String department,
        String batch,
        String hall,
        boolean enabled,
        List<String> roles) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getDepartment(),
                user.getBatch(),
                user.getHall(),
                user.isEnabled(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .sorted()
                        .toList());
    }
}
