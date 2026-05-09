package com.smartgarden.integration.messaging.correlation;

import com.smartgarden.integration.dto.messaging.AmqpPlantResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Correlaciona requests AMQP de saída com suas respostas assíncronas.
 *
 * <p>Fluxo:
 * <ol>
 *   <li>{@code PlantService} chama {@link #register} antes de publicar;</li>
 *   <li>{@code PlantResponseConsumer} chama {@link #resolve} ao receber a resposta;</li>
 *   <li>Em caso de timeout, {@code PlantService} chama {@link #remove} para cleanup.</li>
 * </ol>
 *
 * <p>Limitação conhecida: se o timeout expirar mas {@link #remove} não for chamado
 * (ex: interrupção da thread), o {@code CompletableFuture} permanece no mapa até
 * o próximo restart. Para produção, substituir o {@code ConcurrentHashMap} por
 * um cache com TTL (ex: Caffeine) para limpeza automática.
 * TODO: implementar cleanup periódico via @Scheduled ou Caffeine com TTL.
 */
@Slf4j
@Component
public class PendingRequestRegistry {

    private final ConcurrentHashMap<String, CompletableFuture<AmqpPlantResponseDto>> pending =
            new ConcurrentHashMap<>();

    /**
     * Registra um request pendente e retorna o future associado.
     */
    public CompletableFuture<AmqpPlantResponseDto> register(String correlationId) {
        CompletableFuture<AmqpPlantResponseDto> future = new CompletableFuture<>();
        pending.put(correlationId, future);
        log.debug("Registered pending request: {}", correlationId);
        return future;
    }

    /**
     * Resolve o future associado ao correlationId.
     *
     * @return {@code true} se havia um request pendente e foi resolvido
     */
    public boolean resolve(String correlationId, AmqpPlantResponseDto response) {
        CompletableFuture<AmqpPlantResponseDto> future = pending.remove(correlationId);
        if (future != null) {
            future.complete(response);
            log.debug("Resolved pending request: {}", correlationId);
            return true;
        }
        return false;
    }

    /**
     * Remove e cancela um request pendente (usado em timeout ou shutdown).
     */
    public void remove(String correlationId) {
        CompletableFuture<AmqpPlantResponseDto> future = pending.remove(correlationId);
        if (future != null) {
            future.cancel(true);
            log.debug("Removed (cancelled) pending request: {}", correlationId);
        }
    }

    public int pendingCount() {
        return pending.size();
    }
}
