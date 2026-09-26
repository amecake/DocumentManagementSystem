package at.fhtw.swen3.paperless.exception;

/** Business layer: uploaded file is missing or not a PDF. */
public class InvalidFileException extends RuntimeException {
    public InvalidFileException(String message) {
        super(message);
    }
}
