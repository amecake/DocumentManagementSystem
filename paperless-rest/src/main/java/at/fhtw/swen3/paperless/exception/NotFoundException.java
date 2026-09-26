package at.fhtw.swen3.paperless.exception;

/** Business layer: requested resource does not exist. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
