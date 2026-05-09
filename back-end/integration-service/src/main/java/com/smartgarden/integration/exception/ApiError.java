package com.smartgarden.integration.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Corpo padrão de todas as respostas de erro da API REST.
 * Retornado pelo GlobalExceptionHandler em qualquer situação de erro.
 */
@Getter
@Builder
public class ApiError {

  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private final Instant timestamp;

  private final int status;
  private final String error;
  private final String message;
  private final String path;
}
