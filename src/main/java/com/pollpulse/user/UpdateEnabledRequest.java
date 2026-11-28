package com.pollpulse.user;

import jakarta.validation.constraints.NotNull;

public record UpdateEnabledRequest(
        @NotNull
        Boolean enabled) {
}
