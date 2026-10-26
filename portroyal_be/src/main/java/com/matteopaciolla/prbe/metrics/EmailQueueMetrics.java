package com.matteopaciolla.prbe.metrics;

import com.matteopaciolla.prbe.repository.EmailRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailQueueMetrics implements MeterBinder {

    private final EmailRepository emailRepository;

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("portroyal_email_queue_pending", emailRepository, EmailRepository::countBySentFalse)
                .description("Number of queued emails that have not been sent")
                .register(registry);
    }
}
