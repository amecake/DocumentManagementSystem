package at.fhtw.swen3.paperless.messaging;

/**
 * Publishes document events. The service layer only knows this interface,
 * so the transport (RabbitMQ) can be replaced, e.g. by a no-op publisher in tests.
 */
public interface DocumentEventPublisher {

    /**
     * @return true if the event was handed over to the broker, false if sending failed
     */
    boolean publish(DocumentUploadedEvent event);
}
