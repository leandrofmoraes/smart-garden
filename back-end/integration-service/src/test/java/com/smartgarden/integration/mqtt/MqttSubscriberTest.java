package com.smartgarden.integration.mqtt;

//import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgarden.integration.dto.device.IrrigationReadingDto;
import com.smartgarden.integration.mqtt.subscriber.MqttSubscriber;
import com.smartgarden.integration.service.DeviceService;
import com.smartgarden.integration.service.ReadingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MqttSubscriberTest {

  @Mock
  ReadingService readingService;
  @Mock
  DeviceService deviceService;

  private MqttSubscriber subscriber;

  @BeforeEach
  void setUp() {
    // ObjectMapper mapper = new ObjectMapper().registerModule(new
    // JavaTimeModule());

    // subscriber = new MqttSubscriber(mapper, readingService, deviceService);
    subscriber = new MqttSubscriber(readingService, deviceService);
  }

  @Test
  void shouldRouteReadingToReadingService() throws Exception {
    String payload = """
        {"humidity": 65.5, "regando": false, "esp_ip": "192.168.1.10", "esp_rssi": -60}
        """;
    Message<String> msg = MessageBuilder.withPayload(payload)
        .setHeader(MqttHeaders.RECEIVED_TOPIC, "smartgarden/devices/esp-01/reading")
        .build();

    subscriber.handleMqttMessage(msg);

    ArgumentCaptor<IrrigationReadingDto> captor = ArgumentCaptor.forClass(IrrigationReadingDto.class);
    verify(readingService, times(1)).process(captor.capture());

    IrrigationReadingDto captured = captor.getValue();
    assertThat(captured.getDeviceId()).isEqualTo("esp-01");
    assertThat(captured.getHumidity()).isEqualTo(65.5);
    assertThat(captured.getTimestamp()).isNotNull();
  }

  @Test
  void shouldRouteStatusToDeviceService() throws Exception {
    String payload = """
        {"ip": "192.168.1.10", "rssi": -55}
        """;
    Message<String> msg = MessageBuilder.withPayload(payload)
        .setHeader(MqttHeaders.RECEIVED_TOPIC, "smartgarden/devices/esp-01/status")
        .build();

    subscriber.handleMqttMessage(msg);

    verify(deviceService, times(1)).updateStatus(eq("esp-01"), any());
  }

  @Test
  void shouldRouteTelemetryToRegisterIfAbsent() {
    Message<String> msg = MessageBuilder.withPayload("{}")
        .setHeader(MqttHeaders.RECEIVED_TOPIC, "smartgarden/devices/esp-01/telemetry")
        .build();

    subscriber.handleMqttMessage(msg);

    verify(deviceService, times(1)).registerIfAbsent("esp-01", null);
    verifyNoInteractions(readingService);
  }

  @Test
  void shouldIgnoreMessageWithNullTopic() {
    Message<String> msg = MessageBuilder.withPayload("{}").build();

    subscriber.handleMqttMessage(msg);

    verifyNoInteractions(readingService);
    verifyNoInteractions(deviceService);
  }

  @Test
  void shouldIgnoreMessageWithInvalidTopicFormat() {
    Message<String> msg = MessageBuilder.withPayload("{}")
        .setHeader(MqttHeaders.RECEIVED_TOPIC, "invalid/topic")
        .build();

    subscriber.handleMqttMessage(msg);

    verifyNoInteractions(readingService);
    verifyNoInteractions(deviceService);
  }

  @Test
  void shouldIgnoreUnknownTopicSuffix() {
    Message<String> msg = MessageBuilder.withPayload("{}")
        .setHeader(MqttHeaders.RECEIVED_TOPIC, "smartgarden/devices/esp-01/unknown")
        .build();

    subscriber.handleMqttMessage(msg);

    verifyNoInteractions(readingService);
    verifyNoInteractions(deviceService);
  }
}
