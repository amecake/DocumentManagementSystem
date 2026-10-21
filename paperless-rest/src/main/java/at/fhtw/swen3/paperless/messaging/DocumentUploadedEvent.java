package at.fhtw.swen3.paperless.messaging;

public record DocumentUploadedEvent(Long documentId, String title, String filename) {
}
