package com.smartgarden.integration.service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.smartgarden.integration.dto.device.IrrigationReadingDto;
import com.smartgarden.integration.dto.messaging.AmqpPlantRequestDto;
import com.smartgarden.integration.dto.messaging.AmqpPlantResponseDto;
import com.smartgarden.integration.dto.plant.PlantRequestDto;
import com.smartgarden.integration.dto.plant.PlantResponseDto;
import com.smartgarden.integration.exception.IntegrationException;
import com.smartgarden.integration.messaging.correlation.PendingRequestRegistry;
import com.smartgarden.integration.messaging.publisher.PlantAmqpPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Orquestra operações CRUD de plantas via AMQP (request-reply com
 * correlationId).
 *
 * <p>
 * Cada método público publica um request, bloqueia a thread HTTP pelo tempo
 * máximo de {@code amqp.reply.timeout-ms} e lança {@link IntegrationException}
 * em caso de timeout ou erro reportado pelo plant-management-service.
 *
 * <p>
 * Limitação conhecida: o bloqueio de thread impacta throughput em alta carga.
 * Para TCC, é a solução mais simples sem WebFlux.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlantService {

  private final PlantAmqpPublisher plantAmqpPublisher;
  private final PendingRequestRegistry pendingRequestRegistry;
  // private final ObjectMapper objectMapper;

  private final JsonMapper mapper = JsonMapper.builder()
      .findAndAddModules()
      .build();

  @Value("${amqp.reply.timeout-ms:5000}")
  private long replyTimeoutMs;

  // Public API
  public List<PlantResponseDto> listPlants() {
    AmqpPlantResponseDto response = sendAndWait(buildRequest("LIST", null, null));
    return castList(response.getPayload(), PlantResponseDto.class);
  }

  public PlantResponseDto getPlant(String id) {
    AmqpPlantResponseDto response = sendAndWait(buildRequest("GET", id, null));
    return castSingle(response.getPayload(), PlantResponseDto.class);
  }

  public PlantResponseDto createPlant(PlantRequestDto dto) {
    AmqpPlantResponseDto response = sendAndWait(buildRequest("CREATE", null, dto));
    return castSingle(response.getPayload(), PlantResponseDto.class);
  }

  public PlantResponseDto updatePlant(String id, PlantRequestDto dto) {
    AmqpPlantResponseDto response = sendAndWait(buildRequest("UPDATE", id, dto));
    return castSingle(response.getPayload(), PlantResponseDto.class);
  }

  public void deletePlant(String id) {
    sendAndWait(buildRequest("DELETE", id, null));
  }

  /**
   * Busca leituras históricas de um device via AMQP quando o ReadingCache está
   * vazio.
   * Usa a mesma infra de request-reply já existente para plantas.
   *
   * @param deviceKey identificador MQTT do dispositivo (ex: "esp-01")
   * @return lista de leituras (pode ser vazia)
   */
  public List<IrrigationReadingDto> getReadingsByDeviceFromDomain(String deviceKey) {
    // Reutiliza o campo "plantId" do envelope para carregar o deviceKey
    AmqpPlantResponseDto response = sendAndWait(buildRequest("GET_READINGS_BY_DEVICE", deviceKey, null));
    return castList(response.getPayload(), IrrigationReadingDto.class);
  }

  private AmqpPlantRequestDto buildRequest(String action, String plantId, Object payload) {
    return AmqpPlantRequestDto.builder()
        .correlationId(UUID.randomUUID().toString())
        .action(action)
        .plantId(plantId)
        .payload(payload)
        .build();
  }

  private AmqpPlantResponseDto sendAndWait(AmqpPlantRequestDto request) {
    CompletableFuture<AmqpPlantResponseDto> future = pendingRequestRegistry.register(request.getCorrelationId());

    plantAmqpPublisher.publish(request);

    try {
      AmqpPlantResponseDto response = future.get(replyTimeoutMs, TimeUnit.MILLISECONDS);

      if (!response.isSuccess()) {
        throw new IntegrationException(
            "plant-management-service error: " + response.getErrorMessage());
      }
      return response;

    } catch (TimeoutException e) {
      pendingRequestRegistry.remove(request.getCorrelationId());
      log.warn("Timeout waiting for plant-management-service [action={}, correlationId={}]",
          request.getAction(), request.getCorrelationId());
      throw new IntegrationException(
          "Timeout waiting for plant-management-service response [action="
              + request.getAction() + "]",
          e);

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      pendingRequestRegistry.remove(request.getCorrelationId());
      log.error("Request interrupted [action={}, correlationId={}]",
          request.getAction(), request.getCorrelationId());
      throw new IntegrationException("Request interrupted", e);

    } catch (IntegrationException e) {
      throw e;

    } catch (Exception e) {
      log.error("Unexpected error in AMQP request-reply [action={}, correlationId={}]: {}",
          request.getAction(), request.getCorrelationId(), e.getMessage(), e);
      throw new IntegrationException("Unexpected error during AMQP request-reply", e);
    }
  }

  private <T> T castSingle(Object raw, Class<T> type) {
    try {
      // return objectMapper.convertValue(raw, type);
      return mapper.convertValue(raw, type);
    } catch (IllegalArgumentException e) {
      throw new IntegrationException(
          "Failed to deserialize AMQP response as " + type.getSimpleName(), e);
    }
  }

  private <T> List<T> castList(Object raw, Class<T> elementType) {
    if (raw == null)
      return List.of();
    try {
      // var listType = objectMapper.getTypeFactory()
      var listType = mapper.getTypeFactory()
          .constructCollectionType(List.class, elementType);
      return mapper.convertValue(raw, listType);
    } catch (IllegalArgumentException e) {
      throw new IntegrationException(
          "Failed to deserialize AMQP response as List<"
              + elementType.getSimpleName() + ">",
          e);
    }
  }
}
