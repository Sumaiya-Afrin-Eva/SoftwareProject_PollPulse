package com.pollpulse.user;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record UpdateRolesRequest(
        @NotEmpty
        List<String> roles) {
}
