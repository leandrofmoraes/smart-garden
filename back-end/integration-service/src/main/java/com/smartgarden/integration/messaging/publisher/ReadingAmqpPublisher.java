package com.smartgarden.integration.messaging.publisher;

import com.smartgarden.integration.dto.device.IrrigationReadingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadingAmqpPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${amqp.exchange}")
    private String exchange;

    @Value("${amqp.routing-key.reading-ingest}")
    private String readingIngestKey;

    public void publish(IrrigationReadingDto reading) {
        log.debug("Forwarding reading via AMQP for device {}", reading.getDeviceId());
        rabbitTemplate.convertAndSend(exchange, readingIngestKey, reading);
    }
}
