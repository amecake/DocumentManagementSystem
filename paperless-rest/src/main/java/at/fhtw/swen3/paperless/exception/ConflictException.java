package at.fhtw.swen3.paperless.exception;

/** The request conflicts with the current state, e.g. a tag that is already on the document. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
