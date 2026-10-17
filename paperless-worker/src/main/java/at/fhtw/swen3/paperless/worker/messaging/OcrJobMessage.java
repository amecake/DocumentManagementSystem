package at.fhtw.swen3.paperless.worker.messaging;

public record OcrJobMessage(Long documentId, String filename) {
}
