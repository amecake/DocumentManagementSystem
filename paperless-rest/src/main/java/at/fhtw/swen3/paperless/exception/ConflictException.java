package at.fhtw.swen3.paperless.exception;

/** The request conflicts with the current state, e.g. a username that is already taken. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
