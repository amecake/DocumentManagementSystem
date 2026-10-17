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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitOcrJobPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publish_sendsJobToOcrQueue() {
        var publisher = new RabbitOcrJobPublisher(rabbitTemplate, "ocr.queue");
        var job = new OcrJobMessage(1L, "a.pdf");

        assertThat(publisher.publish(job)).isTrue();

        verify(rabbitTemplate).convertAndSend("ocr.queue", job);
    }

    @Test
    void publish_brokerDown_returnsFalseInsteadOfThrowing() {
        var publisher = new RabbitOcrJobPublisher(rabbitTemplate, "ocr.queue");
        doThrow(new AmqpConnectException(new ConnectException("refused")))
                .when(rabbitTemplate).convertAndSend(eq("ocr.queue"), any(Object.class));

        assertThat(publisher.publish(new OcrJobMessage(1L, "a.pdf"))).isFalse();
    }
}
