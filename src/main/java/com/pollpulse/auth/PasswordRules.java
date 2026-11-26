package com.pollpulse.auth;

public final class PasswordRules {

    public static final String PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";
    public static final String ERROR_MESSAGE =
            "Password must be at least 8 characters and include an uppercase letter, "
                    + "a lowercase letter, a digit, and a special character.";

    private PasswordRules() {
    }
}
