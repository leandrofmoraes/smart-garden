package com.smartgarden.plantmanagement.service;

import com.smartgarden.plantmanagement.dto.irrigation.IrrigationDecisionDto;
import com.smartgarden.plantmanagement.model.PlantCareModel;
import com.smartgarden.plantmanagement.model.ReadingModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class IrrigationServiceTest {

  private IrrigationService irrigationService;
  private static final String PLANT_ID = "plant-uuid-123";

  @BeforeEach
  void setUp() {
    irrigationService = new IrrigationService();
    ReflectionTestUtils.setField(irrigationService, "defaultMinSoilMoisture", 30.0);
    ReflectionTestUtils.setField(irrigationService, "defaultMaxSoilMoisture", 70.0);
  }

  @Test
  void shouldDecideIrrigateWhenHumidityBelowMin() {
    ReadingModel reading = reading("esp-01", 20.0);
    PlantCareModel care = care(30.0, 70.0);

    IrrigationDecisionDto decision = irrigationService.evaluate(reading, care, PLANT_ID);

    assertThat(decision.getShouldIrrigate()).isTrue();
    assertThat(decision.getReason()).isEqualTo("LOW_MOISTURE");
    assertThat(decision.getDeviceId()).isEqualTo("esp-01");
    assertThat(decision.getPlantId()).isEqualTo(PLANT_ID);
  }

  @Test
  void shouldCalculateCorrectTargetHumidity() {
    ReadingModel reading = reading("esp-01", 20.0);
    PlantCareModel care = care(30.0, 70.0);

    IrrigationDecisionDto decision = irrigationService.evaluate(reading, care, PLANT_ID);

    // target = 30 + (70-30)*0.6 = 30 + 24 = 54.0
    assertThat(decision.getTargetHumidity()).isEqualTo(54.0);
    assertThat(decision.getMinHumidity()).isEqualTo(30.0);
    assertThat(decision.getMaxHumidity()).isEqualTo(70.0);
    assertThat(decision.getCurrentHumidity()).isEqualTo(20.0);
  }

  @Test
  void shouldReturnAdequateWhenHumidityWithinRange() {
    ReadingModel reading = reading("esp-01", 50.0);
    PlantCareModel care = care(30.0, 70.0);

    IrrigationDecisionDto decision = irrigationService.evaluate(reading, care, PLANT_ID);

    assertThat(decision.getShouldIrrigate()).isFalse();
    assertThat(decision.getReason()).isEqualTo("ADEQUATE");
  }

  @Test
  void shouldReturnHighMoistureWhenAboveMax() {
    ReadingModel reading = reading("esp-01", 85.0);
    PlantCareModel care = care(30.0, 70.0);

    IrrigationDecisionDto decision = irrigationService.evaluate(reading, care, PLANT_ID);

    assertThat(decision.getShouldIrrigate()).isFalse();
    assertThat(decision.getReason()).isEqualTo("HIGH_MOISTURE");
  }

  @Test
  void shouldReturnNoDataWhenHumidityIsNull() {
    ReadingModel reading = reading("esp-01", null);

    IrrigationDecisionDto decision = irrigationService.evaluate(reading, null, PLANT_ID);

    assertThat(decision.getShouldIrrigate()).isFalse();
    assertThat(decision.getReason()).isEqualTo("NO_DATA");
    assertThat(decision.getDeviceId()).isEqualTo("esp-01");
  }

  @Test
  void shouldUseCareDataThresholdNotDefault() {
    // planta exótica com limiar diferente do default
    ReadingModel reading = reading("esp-01", 10.0);
    PlantCareModel care = care(15.0, 40.0);

    IrrigationDecisionDto decision = irrigationService.evaluate(reading, care, PLANT_ID);

    assertThat(decision.getShouldIrrigate()).isTrue();
    assertThat(decision.getMinHumidity()).isEqualTo(15.0);
    // NÃO usa o default 30.0
  }

  @Test
  void shouldUseDefaultsWhenCareIsNull() {
    ReadingModel reading = reading("esp-01", 20.0); // abaixo do default 30
    IrrigationDecisionDto decision = irrigationService.evaluate(reading, null, PLANT_ID);

    assertThat(decision.getShouldIrrigate()).isTrue();
    assertThat(decision.getReason()).isEqualTo("NO_CARE_DATA");
    assertThat(decision.getMinHumidity()).isEqualTo(30.0);
  }

  @Test
  void shouldReturnAdequateUsingDefaultsWhenHumidityIs50() {
    ReadingModel reading = reading("esp-01", 50.0);
    IrrigationDecisionDto decision = irrigationService.evaluate(reading, null, null);

    assertThat(decision.getShouldIrrigate()).isFalse();
  }

  @Test
  void shouldHandleNullPlantId() {
    ReadingModel reading = reading("esp-01", 20.0);
    IrrigationDecisionDto decision = irrigationService.evaluate(reading, null, null);

    assertThat(decision.getPlantId()).isNull();
    assertThat(decision.getDeviceId()).isEqualTo("esp-01");
  }

  private ReadingModel reading(String deviceKey, Double humidity) {
    return ReadingModel.builder().deviceKey(deviceKey).humidity(humidity).build();
  }

  private PlantCareModel care(double min, double max) {
    return PlantCareModel.builder().soilMoistureMin(min).soilMoistureMax(max).build();
  }
}
