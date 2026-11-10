package at.fhtw.swen3.paperless.messaging;

/**
 * Publishes document events. The service layer only knows this interface,
 * so the transport (RabbitMQ) can be replaced, e.g. by a no-op publisher in tests.
 */
public interface DocumentEventPublisher {

    void publish(DocumentUploadedEvent event);
}
