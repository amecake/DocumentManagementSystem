package at.fhtw.swen3.paperless.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentUploadedPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publish_sendsToTheOcrQueueThroughTheDefaultExchange() {
        var event = new DocumentUploadedEvent(1L, "Invoice", "invoice.pdf", "key-1");

        boolean sent = new DocumentUploadedPublisher(rabbitTemplate).publish(event);

        verify(rabbitTemplate).convertAndSend("", "paperless.ocr", event);
        assertThat(sent).isTrue();
    }

    @Test
    void publish_rabbitDown_doesNotThrowAndReturnsFalse() {
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        boolean sent = new DocumentUploadedPublisher(rabbitTemplate)
                .publish(new DocumentUploadedEvent(1L, "Invoice", "invoice.pdf", "key-1"));

        assertThat(sent).isFalse();
    }
}
