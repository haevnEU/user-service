package de.haevn.userservice.v2.exception;

public class InvalidCredentials extends RuntimeException {
    public InvalidCredentials() {
        this("Invalid credentials.");
    }

    public InvalidCredentials(String message) {
        super(message);
    }
}
