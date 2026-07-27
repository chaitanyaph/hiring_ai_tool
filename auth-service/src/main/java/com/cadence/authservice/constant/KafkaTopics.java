package com.cadence.authservice.constant;

/**
 * Kafka topic names published by the Auth Service, consumed downstream by
 * Notification Service (emails), Analytics Service, and Audit pipelines,
 * per the platform's Event Driven Architecture (Volume 8).
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String USER_REGISTERED       = "auth.user.registered";
    public static final String USER_LOGGED_IN         = "auth.user.logged-in";
    public static final String PASSWORD_RESET_REQUESTED = "auth.password.reset-requested";
    public static final String PASSWORD_CHANGED       = "auth.password.changed";
    public static final String ACCOUNT_LOCKED         = "auth.account.locked";
    public static final String EMAIL_VERIFICATION_REQUESTED = "auth.email.verification-requested";
    public static final String EMAIL_VERIFIED         = "auth.email.verified";
    public static final String MFA_ENABLED            = "auth.mfa.enabled";
}
