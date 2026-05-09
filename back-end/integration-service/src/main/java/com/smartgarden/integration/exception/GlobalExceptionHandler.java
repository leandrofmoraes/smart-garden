package com.smartgarden.integration.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Centraliza o tratamento de exceções e retorna sempre um ApiError padronizado.
 * Nenhum stacktrace é exposto no corpo da resposta.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 404 — recurso não encontrado no cache local (device, leitura).
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex,
      HttpServletRequest request) {
    log.warn("Resource not found [path={}]: {}", request.getRequestURI(), ex.getMessage());
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
  }

  /**
   * 502 — falha de integração: timeout AMQP, erro upstream, falha MQTT.
   */
  @ExceptionHandler(IntegrationException.class)
  public ResponseEntity<ApiError> handleIntegration(IntegrationException ex,
      HttpServletRequest request) {
    log.error("Integration error [path={}]: {}", request.getRequestURI(), ex.getMessage(), ex);
    return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), request.getRequestURI());
  }

  /**
   * 400 — falha de validação Jakarta (@NotBlank, @Valid, etc.).
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
      HttpServletRequest request) {
    String fields = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
            (a, b) -> a))
        .toString();

    String message = "Validation failed: " + fields;
    log.warn("Validation error [path={}]: {}", request.getRequestURI(), message);
    return build(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
  }

  /**
   * 500 — catch-all para erros inesperados. Nunca expõe stacktrace no body.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
    log.error("Unexpected error [path={}]: {}", request.getRequestURI(), ex.getMessage(), ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred.", request.getRequestURI());
  }

  private ResponseEntity<ApiError> build(HttpStatus status, String message, String path) {
    ApiError error = ApiError.builder()
        .timestamp(Instant.now())
        .status(status.value())
        .error(status.getReasonPhrase())
        .message(message)
        .path(path)
        .build();
    return ResponseEntity.status(status).body(error);
  }
}
