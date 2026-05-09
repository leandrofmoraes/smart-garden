package com.smartgarden.integration.exception;

/**
 * Lançada quando um recurso não é encontrado no cache local (device, leitura).
 * Mapeada para HTTP 404 no GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
