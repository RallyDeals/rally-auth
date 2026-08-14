package com.rally.auth.messaging.contract;

/**
 * Outbox event types published on the {@code user.events} topic.
 */
public final class UserEventTypes {

    public static final String TOPIC = "user.events";
    public static final String AGGREGATE_TYPE = "User";

    public static final String REGISTERED = "User.Registered";
    public static final String EMAIL_VERIFICATION_REQUESTED = "User.EmailVerificationRequested";

    private UserEventTypes() {
    }
}