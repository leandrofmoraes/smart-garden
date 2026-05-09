package com.smartgarden.plantmanagement.repository;

import com.smartgarden.plantmanagement.model.DeviceModel;
import com.smartgarden.plantmanagement.model.PlantModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class DeviceRepositoryTest {

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
  DeviceRepository deviceRepository;
  @Autowired
  PlantRepository plantRepository;

  @Test
  void shouldSaveAndFindDeviceByKey() {
    DeviceModel device = DeviceModel.builder()
        .deviceKey("esp-01").name("Sensor Jardim")
        .ip("192.168.1.10").online(true).lastSeen(Instant.now())
        .build();
    deviceRepository.save(device);

    Optional<DeviceModel> found = deviceRepository.findByDeviceKey("esp-01");
    assertThat(found).isPresent();
    assertThat(found.get().getIp()).isEqualTo("192.168.1.10");
  }

  @Test
  void shouldFindDeviceWithPlantUsingJoinFetch() {
    PlantModel plant = plantRepository.save(
        PlantModel.builder().name("Orquídea").build());

    DeviceModel device = DeviceModel.builder()
        .deviceKey("esp-02").plant(plant).online(true).lastSeen(Instant.now())
        .build();
    deviceRepository.save(device);

    Optional<DeviceModel> found = deviceRepository.findByDeviceKeyWithPlant("esp-02");

    assertThat(found).isPresent();
    assertThat(found.get().getPlant()).isNotNull();
    assertThat(found.get().getPlant().getName()).isEqualTo("Orquídea");
  }

  @Test
  void shouldReturnEmptyForUnknownDeviceKey() {
    assertThat(deviceRepository.findByDeviceKeyWithPlant("not-existing")).isEmpty();
  }

  @Test
  void shouldSaveDeviceWithoutPlant() {
    DeviceModel device = DeviceModel.builder()
        .deviceKey("esp-new").online(false).lastSeen(Instant.now())
        .build();
    deviceRepository.save(device);

    Optional<DeviceModel> found = deviceRepository.findByDeviceKeyWithPlant("esp-new");
    assertThat(found).isPresent();
    assertThat(found.get().getPlant()).isNull();
  }
}
