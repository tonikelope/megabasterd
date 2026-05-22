package com.tonikelope.megabasterd.core;

public final class AuthResult {

    public enum Status {
        SUCCESS,
        CANCELLED,
        FAILED,
        CHALLENGE_REQUIRED
    }

    private final Status status;
    private final AuthChallenge challenge;
    private final String message;

    private AuthResult(Status status, AuthChallenge challenge, String message) {
        this.status = status;
        this.challenge = challenge;
        this.message = message;
    }

    public static AuthResult success() {
        return new AuthResult(Status.SUCCESS, null, null);
    }

    public static AuthResult cancelled() {
        return new AuthResult(Status.CANCELLED, null, null);
    }

    public static AuthResult failed(String message) {
        return new AuthResult(Status.FAILED, null, message);
    }

    public static AuthResult challengeRequired(AuthChallenge challenge) {
        return new AuthResult(Status.CHALLENGE_REQUIRED, challenge, null);
    }

    public Status status() {
        return status;
    }

    public AuthChallenge challenge() {
        return challenge;
    }

    public String message() {
        return message;
    }
}
