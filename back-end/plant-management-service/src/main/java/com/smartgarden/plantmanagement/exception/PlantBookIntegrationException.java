package com.smartgarden.plantmanagement.exception;

public class PlantBookIntegrationException extends RuntimeException {
  public PlantBookIntegrationException(String message) {
    super(message);
  }

  public PlantBookIntegrationException(String message, Throwable cause) {
    super(message, cause);
  }
}
