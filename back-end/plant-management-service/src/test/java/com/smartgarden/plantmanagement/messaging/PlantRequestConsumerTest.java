package com.smartgarden.plantmanagement.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartgarden.plantmanagement.dto.messaging.AmqpRequestDto;
import com.smartgarden.plantmanagement.dto.messaging.AmqpResponseDto;
import com.smartgarden.plantmanagement.dto.plant.PlantResponseDto;
import com.smartgarden.plantmanagement.messaging.consumer.PlantRequestConsumer;
import com.smartgarden.plantmanagement.messaging.publisher.PlantResponsePublisher;
import com.smartgarden.plantmanagement.service.PlantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlantRequestConsumerTest {

  @Mock
  PlantService plantService;
  @Mock
  PlantResponsePublisher responsePublisher;
  @Spy
  ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @InjectMocks
  PlantRequestConsumer consumer;

  @Test
  void shouldHandleListAction() {
    PlantResponseDto dto = PlantResponseDto.builder().id("1").name("Samambaia").build();
    when(plantService.listAll()).thenReturn(List.of(dto));

    AmqpRequestDto request = AmqpRequestDto.builder()
        .correlationId("corr-1")
        .action("LIST")
        .build();

    consumer.consume(request);

    ArgumentCaptor<AmqpResponseDto> captor = ArgumentCaptor.forClass(AmqpResponseDto.class);
    verify(responsePublisher).publish(captor.capture());

    AmqpResponseDto response = captor.getValue();
    assertThat(response.getCorrelationId()).isEqualTo("corr-1");
    assertThat(response.isSuccess()).isTrue();
  }

  @Test
  void shouldHandleGetAction() {
    String id = UUID.randomUUID().toString();
    when(plantService.getById(id)).thenReturn(
        PlantResponseDto.builder().id(id).name("Orquídea").build());

    AmqpRequestDto request = AmqpRequestDto.builder()
        .correlationId("corr-2")
        .action("GET")
        .plantId(id)
        .build();

    consumer.consume(request);

    ArgumentCaptor<AmqpResponseDto> captor = ArgumentCaptor.forClass(AmqpResponseDto.class);
    verify(responsePublisher).publish(captor.capture());
    assertThat(captor.getValue().isSuccess()).isTrue();
  }

  @Test
  void shouldReturnErrorForUnknownAction() {
    AmqpRequestDto request = AmqpRequestDto.builder()
        .correlationId("corr-3")
        .action("INVALID_ACTION")
        .build();

    consumer.consume(request);

    ArgumentCaptor<AmqpResponseDto> captor = ArgumentCaptor.forClass(AmqpResponseDto.class);
    verify(responsePublisher).publish(captor.capture());
    assertThat(captor.getValue().isSuccess()).isFalse();
    assertThat(captor.getValue().getErrorMessage()).contains("Unknown action");
  }

  @Test
  void shouldReturnErrorWhenServiceThrows() {
    when(plantService.listAll()).thenThrow(new RuntimeException("DB error"));

    AmqpRequestDto request = AmqpRequestDto.builder()
        .correlationId("corr-4")
        .action("LIST")
        .build();

    consumer.consume(request);

    ArgumentCaptor<AmqpResponseDto> captor = ArgumentCaptor.forClass(AmqpResponseDto.class);
    verify(responsePublisher).publish(captor.capture());
    assertThat(captor.getValue().isSuccess()).isFalse();
    assertThat(captor.getValue().getErrorMessage()).contains("DB error");
  }
}
