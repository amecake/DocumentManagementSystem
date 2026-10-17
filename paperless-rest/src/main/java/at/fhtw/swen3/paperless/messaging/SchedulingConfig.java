package at.fhtw.swen3.paperless.messaging;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// not on PaperlessApplication so the @WebMvcTest/@DataJpaTest tests don't start the scheduler
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
