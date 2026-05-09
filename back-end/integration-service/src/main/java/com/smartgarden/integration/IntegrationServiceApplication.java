package com.smartgarden.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.config.EnableIntegration;

@SpringBootApplication
@EnableIntegration
public class IntegrationServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(IntegrationServiceApplication.class, args);
  }
}
