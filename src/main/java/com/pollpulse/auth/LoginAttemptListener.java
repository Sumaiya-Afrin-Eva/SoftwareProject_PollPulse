package com.pollpulse.auth;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptListener {

    private final LoginAttemptService loginAttemptService;

    public LoginAttemptListener(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @EventListener
    public void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
        loginAttemptService.recordFailure(event.getAuthentication().getName());
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        loginAttemptService.recordSuccess(event.getAuthentication().getName());
    }
}
