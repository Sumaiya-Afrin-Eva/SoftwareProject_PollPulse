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
}
