package at.fhtw.swen3.paperless.worker.messaging;

public record DocumentUploadedEvent(Long documentId, String title, String filename) {
}
