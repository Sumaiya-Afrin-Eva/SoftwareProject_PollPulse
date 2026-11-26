package com.pollpulse.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 2, max = 100)
        String name,
        @NotBlank
        @Email
        String email,
        @NotBlank
        @Pattern(regexp = PasswordRules.PASSWORD_REGEX, message = PasswordRules.ERROR_MESSAGE)
        String password,
        String department,
        String batch,
        String hall) {
}
