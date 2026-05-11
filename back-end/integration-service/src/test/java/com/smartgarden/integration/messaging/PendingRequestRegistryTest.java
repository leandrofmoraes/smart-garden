package com.smartgarden.integration.messaging;

import com.smartgarden.integration.dto.messaging.AmqpPlantResponseDto;
import com.smartgarden.integration.messaging.correlation.PendingRequestRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PendingRequestRegistryTest {

  private PendingRequestRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new PendingRequestRegistry();
  }

  @Test
  void shouldRegisterAndResolveFuture() throws Exception {
    CompletableFuture<AmqpPlantResponseDto> future = registry.register("corr-1");

    boolean resolved = registry.resolve("corr-1", response("corr-1", true));

    assertThat(resolved).isTrue();
    AmqpPlantResponseDto result = future.get(1, TimeUnit.SECONDS);
    assertThat(result.getCorrelationId()).isEqualTo("corr-1");
    assertThat(result.isSuccess()).isTrue();
    assertThat(registry.pendingCount()).isZero();
  }

  @Test
  void shouldReturnFalseForUnknownCorrelationId() {
    assertThat(registry.resolve("unknown", response("unknown", true))).isFalse();
  }

  @Test
  void shouldCancelFutureOnRemove() {
    CompletableFuture<AmqpPlantResponseDto> future = registry.register("corr-timeout");
    registry.remove("corr-timeout");

    assertThat(future.isCancelled()).isTrue();
    assertThat(registry.pendingCount()).isZero();
  }

  @Test
  void shouldHandleRemoveOfNonExistentIdGracefully() {
    // não deve lançar exceção
    registry.remove("does-not-exist");
  }

  @Test
  void shouldTrackMultiplePendingRequests() {
    registry.register("r1");
    registry.register("r2");
    registry.register("r3");
    assertThat(registry.pendingCount()).isEqualTo(3);

    registry.resolve("r2", response("r2", true));
    assertThat(registry.pendingCount()).isEqualTo(2);
  }

  @Test
  void shouldResolveCorrectFutureAmongMultiple() throws Exception {
    CompletableFuture<AmqpPlantResponseDto> f1 = registry.register("r1");
    CompletableFuture<AmqpPlantResponseDto> f2 = registry.register("r2");

    registry.resolve("r2", response("r2", false));

    assertThat(f2.isDone()).isTrue();
    assertThat(f2.get().getCorrelationId()).isEqualTo("r2");
    assertThat(f1.isDone()).isFalse();
  }

  private AmqpPlantResponseDto response(String correlationId, boolean success) {
    return AmqpPlantResponseDto.builder()
        .correlationId(correlationId)
        .success(success)
        .build();
  }
}
