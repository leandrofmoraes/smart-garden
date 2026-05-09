package com.smartgarden.integration.service;

import com.smartgarden.integration.cache.ReadingCache;
import com.smartgarden.integration.dto.device.IrrigationReadingDto;
import com.smartgarden.integration.messaging.publisher.ReadingAmqpPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquestra o fluxo de uma leitura IoT recebida via MQTT:
 * armazena no cache e encaminha para o plant-management-service via AMQP.
 *
 * <p>
 * Criado para desacoplar o MqttSubscriber do ReadingAmqpPublisher.
 * O subscriber conhece apenas este service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingService {

  private final ReadingCache readingCache;
  private final ReadingAmqpPublisher readingAmqpPublisher;

  /**
   * Armazena a leitura no cache e a encaminha via AMQP.
   */
  public void process(IrrigationReadingDto reading) {
    readingCache.put(reading.getDeviceId(), reading);
    readingAmqpPublisher.publish(reading);
    log.info("Reading processed for device {}", reading.getDeviceId());
  }

  public List<IrrigationReadingDto> getReadingsForDevice(String deviceId) {
    return readingCache.getReadingsForDevice(deviceId);
  }

  public List<IrrigationReadingDto> getLatestPerDevice() {
    return readingCache.getLatestPerDevice();
  }
}
