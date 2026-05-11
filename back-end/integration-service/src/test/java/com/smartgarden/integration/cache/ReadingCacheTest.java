package com.smartgarden.integration.cache;

import com.smartgarden.integration.dto.device.IrrigationReadingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ReadingCacheTest {

  private ReadingCache cache;

  @BeforeEach
  void setUp() {
    cache = new ReadingCache();
  }

  @Test
  void shouldStoreAndRetrieveReading() {
    cache.put("device-1", reading("device-1", 60.0));
    assertThat(cache.getReadingsForDevice("device-1")).hasSize(1);
    assertThat(cache.getReadingsForDevice("device-1").get(0).getHumidity()).isEqualTo(60.0);
  }

  @Test
  void shouldReturnEmptyForUnknownDevice() {
    assertThat(cache.getReadingsForDevice("unknown")).isEmpty();
  }

  @Test
  void shouldReturnLatestReadingPerDevice() {
    cache.put("device-1", reading("device-1", 50.0));
    cache.put("device-1", reading("device-1", 75.0));
    cache.put("device-2", reading("device-2", 30.0));

    var latest = cache.getLatestPerDevice();
    assertThat(latest).hasSize(2);

    var d1Latest = latest.stream()
        .filter(r -> "device-1".equals(r.getDeviceId()))
        .findFirst();
    assertThat(d1Latest).isPresent();
    assertThat(d1Latest.get().getHumidity()).isEqualTo(75.0);
  }

  @Test
  void shouldRespectMaxReadingsPerDevice() {
    for (int i = 0; i < 60; i++) {
      cache.put("device-1", reading("device-1", (double) i));
    }
    assertThat(cache.getReadingsForDevice("device-1")).hasSize(50);
  }

  @Test
  void shouldReturnImmutableCopy() {
    cache.put("d1", reading("d1", 10.0));
    var list = cache.getReadingsForDevice("d1");
    assertThat(list).hasSize(1);
    // lista retornada é imutável — não afeta o cache interno
    org.junit.jupiter.api.Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> list.add(reading("d1", 99.0)));
  }

  private IrrigationReadingDto reading(String deviceId, double humidity) {
    return IrrigationReadingDto.builder()
        .deviceId(deviceId)
        .humidity(humidity)
        .timestamp(Instant.now())
        .build();
  }
}
