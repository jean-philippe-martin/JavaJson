package org.example;

public class InvariantException extends RuntimeException {
    public InvariantException(String message) {
        super(message);
    }
    public InvariantException(String message, Throwable cause) {
        super(message, cause);
    }
}
