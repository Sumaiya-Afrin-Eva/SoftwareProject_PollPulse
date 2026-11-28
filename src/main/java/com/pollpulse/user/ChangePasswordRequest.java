package com.pollpulse.user;

import com.pollpulse.auth.PasswordRules;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @NotBlank
        String currentPassword,
        @NotBlank
        @Pattern(regexp = PasswordRules.PASSWORD_REGEX, message = PasswordRules.ERROR_MESSAGE)
        String newPassword) {
}
