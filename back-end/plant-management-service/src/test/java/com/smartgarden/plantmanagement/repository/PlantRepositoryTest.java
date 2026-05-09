package com.smartgarden.plantmanagement.repository;

import com.smartgarden.plantmanagement.model.PlantCareModel;
import com.smartgarden.plantmanagement.model.PlantModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PlantRepositoryTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("smartgarden_test")
      .withUsername("test")
      .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @Autowired
  PlantRepository plantRepository;

  @Autowired
  PlantCareRepository plantCareRepository;

  @Test
  void shouldSaveAndFindPlantWithCare() {
    PlantModel plant = PlantModel.builder()
        .name("Samambaia")
        .scientificName("Nephrolepis exaltata")
        .build();
    PlantModel saved = plantRepository.save(plant);

    PlantCareModel care = PlantCareModel.builder()
        .plant(saved)
        .soilMoistureMin(40.0)
        .soilMoistureMax(70.0)
        .tempMin(16.0)
        .tempMax(26.0)
        .build();
    plantCareRepository.save(care);

    Optional<PlantModel> found = plantRepository.findByIdWithCare(saved.getId());

    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Samambaia");
    assertThat(found.get().getCare()).isNotNull();
    assertThat(found.get().getCare().getSoilMoistureMin()).isEqualTo(40.0);
  }

  @Test
  void shouldReturnEmptyWhenPlantNotFound() {
    Optional<PlantModel> found = plantRepository.findByIdWithCare(
        java.util.UUID.randomUUID());
    assertThat(found).isEmpty();
  }

  @Test
  void shouldFindAllWithCare() {
    PlantModel p1 = plantRepository.save(
        PlantModel.builder().name("Orquídea").build());
    PlantModel p2 = plantRepository.save(
        PlantModel.builder().name("Cactus").build());

    PlantCareModel care1 = PlantCareModel.builder()
        .plant(p1).soilMoistureMin(15.0).soilMoistureMax(65.0).build();
    plantCareRepository.save(care1);

    var plants = plantRepository.findAllWithCare();
    assertThat(plants).hasSizeGreaterThanOrEqualTo(2);
  }
}
