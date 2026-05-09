package com.smartgarden.integration.cache;

import com.smartgarden.integration.dto.messaging.AmqpPlantResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cache em memória para alertas de cuidado de plantas recebidos via AMQP.
 *
 * <p>
 * <b>Dois modos de leitura:</b>
 * <ul>
 * <li>{@link #consumeAlerts()} — leitura destrutiva: retorna os alertas e os
 * remove.
 * Adequado para o endpoint {@code GET /api/dashboard/alerts}, onde cada chamada
 * entrega os alertas acumulados desde a última leitura.</li>
 * <li>{@link #peekAlerts()} — leitura não destrutiva: retorna os alertas sem
 * removê-los.
 * Útil para inspeção sem consumo (ex: testes, monitoramento interno).</li>
 * </ul>
 */
@Slf4j
@Component
public class AlertCache {

  private static final int MAX_ALERTS = 100;

  private final CopyOnWriteArrayList<AmqpPlantResponseDto> alerts = new CopyOnWriteArrayList<>();

  public void addAlert(AmqpPlantResponseDto alert) {
    if (alerts.size() >= MAX_ALERTS) {
      alerts.remove(0);
    }
    alerts.add(alert);
    log.debug("Alert cached, total={}", alerts.size());
  }

  /**
   * Leitura destrutiva — retorna todos os alertas e limpa o cache.
   */
  public List<AmqpPlantResponseDto> consumeAlerts() {
    List<AmqpPlantResponseDto> snapshot = new ArrayList<>(alerts);
    alerts.clear();
    log.debug("Consumed {} alert(s) from cache", snapshot.size());
    return snapshot;
  }

  /**
   * Retorna os alertas sem modificar o cache.
   */
  public List<AmqpPlantResponseDto> peekAlerts() {
    return List.copyOf(alerts);
  }
}
