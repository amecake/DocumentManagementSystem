package at.fhtw.swen3.paperless.persistence.entity;

/**
 * State of the OCR job of a document. PENDING: the message was sent to the queue.
 * FAILED: the message could not be sent (RabbitMQ not reachable). DONE is set by the worker in sprint 4.
 */
public enum OcrStatus {
    PENDING,
    DONE,
    FAILED
}
