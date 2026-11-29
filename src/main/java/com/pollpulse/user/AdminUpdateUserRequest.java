package com.pollpulse.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUpdateUserRequest(
        @NotBlank
        @Size(min = 2, max = 100)
        String name,
        @NotBlank
        @Email
        String email,
        @Size(max = 100)
        String department,
        @Size(max = 100)
        String batch,
        @Size(max = 100)
        String hall) {
}
