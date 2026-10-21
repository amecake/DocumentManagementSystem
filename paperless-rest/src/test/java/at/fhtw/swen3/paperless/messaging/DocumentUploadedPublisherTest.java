package at.fhtw.swen3.paperless.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentUploadedPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publish_sendsToDocumentEventsExchange() {
        var event = new DocumentUploadedEvent(1L, "Invoice", "invoice.pdf");

        new DocumentUploadedPublisher(rabbitTemplate).publish(event);

        verify(rabbitTemplate).convertAndSend("document.events", "document.uploaded", event);
    }

    @Test
    void publish_rabbitDown_doesNotThrow() {
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatCode(() -> new DocumentUploadedPublisher(rabbitTemplate)
                .publish(new DocumentUploadedEvent(1L, "Invoice", "invoice.pdf")))
                .doesNotThrowAnyException();
    }
}
