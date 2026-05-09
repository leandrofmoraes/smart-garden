package com.smartgarden.integration.exception;

/**
 * Lançada em falhas de integração: timeout AMQP, falha de publish MQTT,
 * erro de resposta do plant-management-service.
 * Mapeada para HTTP 502 no GlobalExceptionHandler.
 */
public class IntegrationException extends RuntimeException {

  public IntegrationException(String message) {
    super(message);
  }

  public IntegrationException(String message, Throwable cause) {
    super(message, cause);
  }
}
