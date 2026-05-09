package com.smartgarden.plantmanagement.messaging.consumer;

//import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.smartgarden.plantmanagement.dto.messaging.AmqpRequestDto;
import com.smartgarden.plantmanagement.dto.messaging.AmqpResponseDto;
import com.smartgarden.plantmanagement.dto.plant.PlantRequestDto;
import com.smartgarden.plantmanagement.messaging.publisher.PlantResponsePublisher;
import com.smartgarden.plantmanagement.service.PlantService;
import com.smartgarden.plantmanagement.service.ReadingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome requests CRUD de plantas vindos do integration-service.
 *
 * <p>
 * Rota pelo campo {@code action} e publica a resposta via
 * {@link PlantResponsePublisher} com o mesmo {@code correlationId},
 * permitindo o matching no PendingRequestRegistry do integration-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlantRequestConsumer {

  private final PlantService plantService;
  private final PlantResponsePublisher responsePublisher;
  // private final ObjectMapper objectMapper;
  private final ReadingService readingService;

  private final JsonMapper mapper = JsonMapper.builder()
      .findAndAddModules()
      .build();

  @RabbitListener(queues = "${amqp.queue.plant-request}")
  public void consume(AmqpRequestDto request) {
    log.info("AMQP ← integration-service [action={}, correlationId={}]",
        request.getAction(), request.getCorrelationId());

    AmqpResponseDto response;
    try {
      response = route(request);
    } catch (Exception e) {
      log.error("Error processing plant request [action={}, correlationId={}]: {}",
          request.getAction(), request.getCorrelationId(), e.getMessage(), e);
      response = AmqpResponseDto.error(request.getCorrelationId(), e.getMessage());
    }

    responsePublisher.publish(response);
  }

  private AmqpResponseDto route(AmqpRequestDto request) {
    return switch (request.getAction()) {
      case "LIST" -> AmqpResponseDto.ok(request.getCorrelationId(),
          plantService.listAll());
      case "GET" -> AmqpResponseDto.ok(request.getCorrelationId(),
          plantService.getById(request.getPlantId()));
      case "CREATE" -> AmqpResponseDto.ok(request.getCorrelationId(),
          plantService.create(toPlantRequest(request.getPayload())));
      case "UPDATE" -> AmqpResponseDto.ok(request.getCorrelationId(),
          plantService.update(request.getPlantId(),
              toPlantRequest(request.getPayload())));
      case "DELETE" -> {
        plantService.delete(request.getPlantId());
        yield AmqpResponseDto.ok(request.getCorrelationId(), null);
      }
      case "GET_READINGS_BY_DEVICE" -> {
        // plantId aqui carrega o deviceKey (reaproveitamento de campo no envelope AMQP)
        String deviceKey = request.getPlantId();
        List<?> readings = readingService.getRecentByDeviceKey(deviceKey);
        yield AmqpResponseDto.ok(request.getCorrelationId(), readings);
      }
      default -> AmqpResponseDto.error(request.getCorrelationId(),
          "Unknown action: " + request.getAction());
    };
  }

  // private PlantRequestDto toPlantRequest(Object payload) {
  // return objectMapper.convertValue(payload, PlantRequestDto.class);
  // }
  private PlantRequestDto toPlantRequest(Object payload) {
    if (payload == null) {
      throw new IllegalArgumentException("Payload cannot be null for CREATE/UPDATE");
    }

    return mapper.convertValue(payload, PlantRequestDto.class);
  }
}
