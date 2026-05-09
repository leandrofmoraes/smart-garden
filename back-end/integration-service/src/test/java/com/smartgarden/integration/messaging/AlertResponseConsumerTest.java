package com.smartgarden.integration.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartgarden.integration.cache.AlertCache;
import com.smartgarden.integration.dto.irrigation.IrrigationDecisionDto;
import com.smartgarden.integration.dto.messaging.AmqpPlantResponseDto;
import com.smartgarden.integration.messaging.consumer.AlertResponseConsumer;
import com.smartgarden.integration.service.IrrigationDecisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertResponseConsumerTest {

    @Mock AlertCache alertCache;
    @Mock IrrigationDecisionService irrigationDecisionService;
    @Spy  ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks AlertResponseConsumer consumer;

    /* ------------------------------------------------------------------ */
    /* Decisão de irrigação válida                                          */
    /* ------------------------------------------------------------------ */

    @Test
    void shouldAlwaysAddToCacheAndRouteValidIrrigationDecision() {
        AmqpPlantResponseDto alert = alert(irrigationPayload("esp-01", true, 20.0, 54.0));

        consumer.consume(alert);

        // 1. Cache preservado — invariante inviolável
        verify(alertCache).addAlert(alert);

        // 2. Decisão roteada ao service com campos corretos
        ArgumentCaptor<IrrigationDecisionDto> captor =
                ArgumentCaptor.forClass(IrrigationDecisionDto.class);
        verify(irrigationDecisionService).process(captor.capture());

        IrrigationDecisionDto routed = captor.getValue();
        assertThat(routed.getDeviceId()).isEqualTo("esp-01");
        assertThat(routed.getShouldIrrigate()).isTrue();
        assertThat(routed.getTargetHumidity()).isEqualTo(54.0);
        assertThat(routed.getCurrentHumidity()).isEqualTo(20.0);
        assertThat(routed.getReason()).isEqualTo("LOW_MOISTURE");
    }

    @Test
    void shouldRouteEvenWhenShouldIrrigateIsFalse() {
        // shouldIrrigate=false ainda é uma decisão válida — service decide não publicar MQTT
        AmqpPlantResponseDto alert = alert(irrigationPayload("esp-01", false, 55.0, 54.0));

        consumer.consume(alert);

        verify(alertCache).addAlert(alert);
        verify(irrigationDecisionService).process(any(IrrigationDecisionDto.class));
    }

    /* ------------------------------------------------------------------ */
    /* Alerta genérico — NÃO deve acionar MQTT                             */
    /* ------------------------------------------------------------------ */

    @Test
    void shouldAddToCacheButNotRouteGenericAlertWithoutShouldIrrigate() {
        // Alerta genérico: sem campo shouldIrrigate
        Map<String, Object> payload = Map.of(
                "message", "Planta precisa de atenção",
                "plantId", "plant-xyz"
        );
        AmqpPlantResponseDto alert = alert(payload);

        consumer.consume(alert);

        verify(alertCache).addAlert(alert);
        verifyNoInteractions(irrigationDecisionService);
    }

    @Test
    void shouldNotRouteWhenDeviceIdIsMissing() {
        // shouldIrrigate presente mas sem deviceId — não qualifica
        Map<String, Object> payload = Map.of(
                "shouldIrrigate", true,
                "targetHumidity", 54.0
                // deviceId ausente
        );
        AmqpPlantResponseDto alert = alert(payload);

        consumer.consume(alert);

        verify(alertCache).addAlert(alert);
        verifyNoInteractions(irrigationDecisionService);
    }

    @Test
    void shouldNotRouteWhenTargetHumidityIsMissing() {
        // shouldIrrigate e deviceId presentes, mas targetHumidity ausente
        // Sem targetHumidity o device não sabe quando parar — não publicar é seguro
        Map<String, Object> payload = Map.of(
                "shouldIrrigate", true,
                "deviceId", "esp-01"
                // targetHumidity ausente
        );
        AmqpPlantResponseDto alert = alert(payload);

        consumer.consume(alert);

        verify(alertCache).addAlert(alert);
        verifyNoInteractions(irrigationDecisionService);
    }

    /* ------------------------------------------------------------------ */
    /* Robustez — falha nunca quebra o cache                               */
    /* ------------------------------------------------------------------ */

    @Test
    void shouldAddToCacheWhenPayloadIsNull() {
        AmqpPlantResponseDto alert = AmqpPlantResponseDto.builder()
                .correlationId("corr-1")
                .success(false)
                .payload(null)
                .build();

        consumer.consume(alert);

        verify(alertCache).addAlert(alert);
        verifyNoInteractions(irrigationDecisionService);
    }

    @Test
    void cacheIsAlwaysFedEvenWhenServiceThrows() {
        // Service falha (ex: MQTT broker fora do ar) — cache não pode ser afetado
        doThrow(new RuntimeException("MQTT broker down"))
                .when(irrigationDecisionService).process(any());

        AmqpPlantResponseDto alert = alert(irrigationPayload("esp-01", true, 20.0, 54.0));

        // Não deve lançar exceção — listener deve ACK a mensagem
        consumer.consume(alert);

        // Cache foi alimentado antes da falha
        verify(alertCache).addAlert(alert);
    }

    @Test
    void shouldAddToCacheEvenWhenDeserializationFails() {
        // Payload não é um Map — Jackson não consegue converter
        AmqpPlantResponseDto alert = AmqpPlantResponseDto.builder()
                .success(true)
                .payload("payload-que-nao-é-um-map")
                .build();

        consumer.consume(alert);

        // Cache sempre recebe
        verify(alertCache).addAlert(alert);
        verifyNoInteractions(irrigationDecisionService);
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                              */
    /* ------------------------------------------------------------------ */

    private AmqpPlantResponseDto alert(Object payload) {
        return AmqpPlantResponseDto.builder()
                .correlationId(null)
                .success(true)
                .payload(payload)
                .build();
    }

    /**
     * Monta o payload como Map — exatamente como o Jackson deserializa
     * o JSON vindo do RabbitMQ antes de chegar ao consumer.
     */
    private Map<String, Object> irrigationPayload(String deviceId, boolean shouldIrrigate,
                                                   double current, double target) {
        return Map.of(
                "deviceId", deviceId,
                "shouldIrrigate", shouldIrrigate,
                "currentHumidity", current,
                "minHumidity", 30.0,
                "maxHumidity", 70.0,
                "targetHumidity", target,
                "reason", "LOW_MOISTURE"
        );
    }
}
