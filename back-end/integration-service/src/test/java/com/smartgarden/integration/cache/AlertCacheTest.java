package com.smartgarden.integration.cache;

import com.smartgarden.integration.dto.messaging.AmqpPlantResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertCacheTest {

  private AlertCache cache;

  @BeforeEach
  void setUp() {
    cache = new AlertCache();
  }

  @Test
  void shouldAddAndConsumeAlerts() {
    cache.addAlert(alert("c1"));
    cache.addAlert(alert("c2"));

    var drained = cache.consumeAlerts();
    assertThat(drained).hasSize(2);
    assertThat(cache.peekAlerts()).isEmpty();
  }

  @Test
  void shouldReturnEmptyWhenNoAlerts() {
    assertThat(cache.consumeAlerts()).isEmpty();
  }

  @Test
  void peekShouldNotConsumeAlerts() {
    cache.addAlert(alert("c1"));
    cache.peekAlerts();
    assertThat(cache.peekAlerts()).hasSize(1);
  }

  @Test
  void shouldRespectMaxAlerts() {
    for (int i = 0; i < 110; i++) {
      cache.addAlert(alert("c" + i));
    }
    assertThat(cache.peekAlerts()).hasSize(100);
  }

  private AmqpPlantResponseDto alert(String correlationId) {
    return AmqpPlantResponseDto.builder()
        .correlationId(correlationId)
        .success(false)
        .errorMessage("Plant needs water")
        .build();
  }
}
