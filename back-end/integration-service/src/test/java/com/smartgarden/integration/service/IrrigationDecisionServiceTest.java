package com.smartgarden.integration.service;

import com.smartgarden.integration.cache.DeviceCache;
import com.smartgarden.integration.dto.irrigation.IrrigationDecisionDto;
import com.smartgarden.integration.mqtt.publisher.CommandMqttPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IrrigationDecisionServiceTest {

  @Mock
  CommandMqttPublisher commandMqttPublisher;
  @Mock
  DeviceCache deviceCache;

  @InjectMocks
  IrrigationDecisionService service;

  @Test
  void shouldPublishMqttCommandWithAllRequiredFields() {
    when(deviceCache.exists("esp-01")).thenReturn(true);

    service.process(decision("esp-01", true, 20.0, 30.0, 70.0, 54.0, "LOW_MOISTURE", "plant-123"));

    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(commandMqttPublisher).publishCommand(eq("esp-01"), captor.capture());

    Map<String, Object> cmd = captor.getValue();
    assertThat(cmd.get("type")).isEqualTo("IRRIGATE");
    assertThat(cmd.get("stopAtHumidity")).isEqualTo(54.0);
    assertThat(cmd.get("currentHumidity")).isEqualTo(20.0);
    assertThat(cmd.get("minHumidity")).isEqualTo(30.0);
    assertThat(cmd.get("maxHumidity")).isEqualTo(70.0);
    assertThat(cmd.get("plantId")).isEqualTo("plant-123");
    assertThat(cmd.get("reason")).isEqualTo("LOW_MOISTURE");
  }

  @Test
  void shouldNotPublishWhenShouldIrrigateIsFalse() {
    service.process(decision("esp-01", false, 55.0, 30.0, 70.0, 54.0, "ADEQUATE", null));

    verifyNoInteractions(commandMqttPublisher);
  }

  @Test
  void shouldNotPublishWhenShouldIrrigateIsNull() {
    IrrigationDecisionDto d = IrrigationDecisionDto.builder()
        .deviceId("esp-01")
        .shouldIrrigate(null)
        .currentHumidity(55.0)
        .build();

    service.process(d);

    verifyNoInteractions(commandMqttPublisher);
  }

  @Test
  void shouldPublishEvenWhenDeviceNotInCache() {
    when(deviceCache.exists("esp-new")).thenReturn(false);

    service.process(decision("esp-new", true, 15.0, 30.0, 70.0, 48.0, "LOW_MOISTURE", null));

    verify(commandMqttPublisher).publishCommand(eq("esp-new"), any());
  }

  @Test
  void stopAtHumidityIsTheKeyFieldToPreventLoop() {
    when(deviceCache.exists("esp-02")).thenReturn(true);

    service.process(decision("esp-02", true, 25.0, 40.0, 70.0, 58.0, "LOW_MOISTURE", null));

    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(commandMqttPublisher).publishCommand(eq("esp-02"), captor.capture());

    assertThat(captor.getValue()).containsKey("stopAtHumidity");
    assertThat(captor.getValue().get("stopAtHumidity")).isEqualTo(58.0);
  }

  @Test
  void shouldNotPublishForHighMoisture() {
    service.process(decision("esp-01", false, 85.0, 30.0, 70.0, 54.0, "HIGH_MOISTURE", null));

    verifyNoInteractions(commandMqttPublisher);
  }

  private IrrigationDecisionDto decision(String deviceId, Boolean shouldIrrigate,
      Double current, Double min, Double max,
      Double target, String reason, String plantId) {
    return IrrigationDecisionDto.builder()
        .deviceId(deviceId)
        .shouldIrrigate(shouldIrrigate)
        .currentHumidity(current)
        .minHumidity(min)
        .maxHumidity(max)
        .targetHumidity(target)
        .reason(reason)
        .plantId(plantId)
        .build();
  }
}
