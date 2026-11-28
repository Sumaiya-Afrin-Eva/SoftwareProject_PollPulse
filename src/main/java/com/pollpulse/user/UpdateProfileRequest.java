package com.pollpulse.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank
        @Size(min = 2, max = 100)
        String name,
        @Size(max = 100)
        String department,
        @Size(max = 100)
        String batch,
        @Size(max = 100)
        String hall) {
}
