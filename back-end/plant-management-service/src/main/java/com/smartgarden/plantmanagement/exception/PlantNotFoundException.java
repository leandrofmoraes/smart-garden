package com.smartgarden.plantmanagement.exception;

public class PlantNotFoundException extends RuntimeException {
  public PlantNotFoundException(String id) {
    super("Plant not found: " + id);
  }
}
