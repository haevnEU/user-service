package de.haevn.userservice.v2.exception;

public class OperationFailed extends RuntimeException {
    public OperationFailed() {
        this("Operation failed.");
    }

    public OperationFailed(String message) {
        super(message);
    }
}
