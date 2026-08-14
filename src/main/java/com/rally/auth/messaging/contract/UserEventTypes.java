package com.rally.auth.messaging.contract;


public final class UserEventTypes {

    public static final String TOPIC = "user.events";
    public static final String AGGREGATE_TYPE = "User";

    public static final String REGISTERED = "User.Registered";
    public static final String EMAIL_VERIFICATION_REQUESTED = "User.EmailVerificationRequested";
    public static final String PASSWORD_RESET_REQUESTED = "User.PasswordResetRequested";

    private UserEventTypes() {
    }
}