package at.fhtw.swen3.paperless.messaging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Exchange and routing key a {@link RabbitEventPublisher} sends to. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RabbitPublication {

    String exchange();

    String routingKey();
}
