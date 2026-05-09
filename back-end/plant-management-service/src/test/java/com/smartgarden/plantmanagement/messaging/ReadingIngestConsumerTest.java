package com.smartgarden.plantmanagement.messaging;

import com.smartgarden.plantmanagement.dto.irrigation.IrrigationDecisionDto;
import com.smartgarden.plantmanagement.dto.messaging.AmqpResponseDto;
import com.smartgarden.plantmanagement.dto.reading.IrrigationReadingDto;
import com.smartgarden.plantmanagement.messaging.consumer.ReadingIngestConsumer;
import com.smartgarden.plantmanagement.messaging.publisher.AlertPublisher;
import com.smartgarden.plantmanagement.model.DeviceModel;
import com.smartgarden.plantmanagement.model.PlantCareModel;
import com.smartgarden.plantmanagement.model.PlantModel;
import com.smartgarden.plantmanagement.model.ReadingModel;
import com.smartgarden.plantmanagement.service.AlertService;
import com.smartgarden.plantmanagement.service.IrrigationService;
import com.smartgarden.plantmanagement.service.ReadingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingIngestConsumerTest {

  @Mock
  ReadingService readingService;
  @Mock
  IrrigationService irrigationService;
  @Mock
  AlertService alertService;
  @Mock
  AlertPublisher alertPublisher;

  @InjectMocks
  ReadingIngestConsumer consumer;

  @Test
  void shouldEvaluateIrrigationWithCorrectPlantFromDevice() {
    // Arrange — device com planta vinculada
    PlantCareModel care = PlantCareModel.builder()
        .soilMoistureMin(30.0).soilMoistureMax(70.0).build();
    PlantModel plant = PlantModel.builder()
        .id(UUID.randomUUID()).name("Samambaia").care(care).build();
    DeviceModel device = DeviceModel.builder()
        .deviceKey("esp-01").plant(plant).build();
    ReadingModel reading = ReadingModel.builder()
        .deviceKey("esp-01").humidity(20.0).device(device).build();

    IrrigationDecisionDto decision = IrrigationDecisionDto.builder()
        .deviceId("esp-01").plantId(plant.getId().toString())
        .currentHumidity(20.0).minHumidity(30.0).maxHumidity(70.0)
        .targetHumidity(54.0).shouldIrrigate(true).reason("LOW_MOISTURE").build();

    when(readingService.saveReading(any())).thenReturn(reading);
    when(irrigationService.evaluate(reading, care, plant.getId().toString()))
        .thenReturn(decision);

    // Act
    IrrigationReadingDto dto = IrrigationReadingDto.builder()
        .deviceId("esp-01").humidity(20.0).build();
    consumer.consume(dto);

    // Assert — irrigationService foi chamado com a planta correta
    verify(irrigationService).evaluate(reading, care, plant.getId().toString());

    // Assert — decisão foi publicada ao integration-service
    ArgumentCaptor<AmqpResponseDto> captor = ArgumentCaptor.forClass(AmqpResponseDto.class);
    verify(alertPublisher).publish(captor.capture());
    AmqpResponseDto published = captor.getValue();
    assertThat(published.isSuccess()).isTrue();
    assertThat(published.getPayload()).isInstanceOf(IrrigationDecisionDto.class);

    IrrigationDecisionDto publishedDecision = (IrrigationDecisionDto) published.getPayload();
    assertThat(publishedDecision.getShouldIrrigate()).isTrue();
    assertThat(publishedDecision.getTargetHumidity()).isEqualTo(54.0);
  }

  @Test
  void shouldEvaluateWithDefaultsWhenDeviceHasNoPlant() {
    DeviceModel deviceWithoutPlant = DeviceModel.builder()
        .deviceKey("esp-new").plant(null).build();
    ReadingModel reading = ReadingModel.builder()
        .deviceKey("esp-new").humidity(40.0).device(deviceWithoutPlant).build();

    IrrigationDecisionDto decision = IrrigationDecisionDto.builder()
        .shouldIrrigate(false).reason("ADEQUATE").deviceId("esp-new").build();

    when(readingService.saveReading(any())).thenReturn(reading);
    when(irrigationService.evaluate(reading, null, null)).thenReturn(decision);

    consumer.consume(IrrigationReadingDto.builder()
        .deviceId("esp-new").humidity(40.0).build());

    // irrigation service chamado com care=null, plantId=null
    verify(irrigationService).evaluate(reading, null, null);
    verify(alertPublisher).publish(any());
    // sem alerta porque reason=ADEQUATE
    verifyNoInteractions(alertService);
  }

  @Test
  void shouldCreateAlertWhenIrrigationNeeded() {
    DeviceModel device = DeviceModel.builder().deviceKey("esp-01").plant(null).build();
    ReadingModel reading = ReadingModel.builder()
        .deviceKey("esp-01").humidity(10.0).device(device).build();

    IrrigationDecisionDto decision = IrrigationDecisionDto.builder()
        .shouldIrrigate(true).reason("LOW_MOISTURE")
        .currentHumidity(10.0).minHumidity(30.0).targetHumidity(54.0).build();

    when(readingService.saveReading(any())).thenReturn(reading);
    when(irrigationService.evaluate(any(), any(), any())).thenReturn(decision);

    consumer.consume(IrrigationReadingDto.builder().deviceId("esp-01").humidity(10.0).build());

    // alerta deve ser persistido
    verify(alertService).createIrrigationAlert(reading, null, decision);
  }

  @Test
  void shouldNotCreateAlertWhenAdequate() {
    DeviceModel device = DeviceModel.builder().deviceKey("esp-01").plant(null).build();
    ReadingModel reading = ReadingModel.builder()
        .deviceKey("esp-01").humidity(50.0).device(device).build();

    IrrigationDecisionDto decision = IrrigationDecisionDto.builder()
        .shouldIrrigate(false).reason("ADEQUATE").build();

    when(readingService.saveReading(any())).thenReturn(reading);
    when(irrigationService.evaluate(any(), any(), any())).thenReturn(decision);

    consumer.consume(IrrigationReadingDto.builder().deviceId("esp-01").humidity(50.0).build());

    verifyNoInteractions(alertService);
  }

  @Test
  void shouldNotThrowWhenReadingServiceFails() {
    when(readingService.saveReading(any())).thenThrow(new RuntimeException("DB error"));

    // não deve propagar exceção — apenas loga
    consumer.consume(IrrigationReadingDto.builder().deviceId("esp-err").humidity(50.0).build());

    verifyNoInteractions(irrigationService);
    verifyNoInteractions(alertPublisher);
  }
}
