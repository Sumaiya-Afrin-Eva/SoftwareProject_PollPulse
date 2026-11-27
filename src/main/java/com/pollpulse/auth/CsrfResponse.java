package com.pollpulse.auth;

public record CsrfResponse(String headerName, String token) {
}
