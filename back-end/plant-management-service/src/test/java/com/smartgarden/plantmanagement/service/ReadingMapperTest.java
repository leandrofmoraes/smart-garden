package com.smartgarden.plantmanagement.service;

import com.smartgarden.plantmanagement.dto.reading.IrrigationReadingDto;
import com.smartgarden.plantmanagement.mapper.ReadingMapper;
import com.smartgarden.plantmanagement.model.DeviceModel;
import com.smartgarden.plantmanagement.model.ReadingModel;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class ReadingMapperTest {

  private final ReadingMapper mapper = new ReadingMapper();

  @Test
  void shouldMapAllFields() {
    DeviceModel device = DeviceModel.builder().deviceKey("esp-01").build();
    IrrigationReadingDto dto = IrrigationReadingDto.builder()
        .id("ext-1").deviceId("esp-01").humidity(65.0)
        .regando(false).espIp("192.168.1.10").espRssi(-60)
        .timestamp(Instant.now()).build();

    ReadingModel model = mapper.toModel(dto, device);

    assertThat(model.getExternalId()).isEqualTo("ext-1");
    assertThat(model.getDeviceKey()).isEqualTo("esp-01");
    assertThat(model.getHumidity()).isEqualTo(65.0);
    assertThat(model.getDevice()).isSameAs(device);
  }

  @Test
  void shouldUseDeviceKeyFromDtoWhenDeviceIsNull() {
    IrrigationReadingDto dto = IrrigationReadingDto.builder()
        .deviceId("esp-fallback").humidity(50.0).build();

    ReadingModel model = mapper.toModel(dto, null);
    assertThat(model.getDeviceKey()).isEqualTo("esp-fallback");
    assertThat(model.getDevice()).isNull();
  }

  @Test
  void shouldThrowWhenBothDeviceKeyAndDtoDeviceIdAreBlank() {
    IrrigationReadingDto dto = IrrigationReadingDto.builder().build();
    assertThatThrownBy(() -> mapper.toModel(dto, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("deviceKey");
  }
}
