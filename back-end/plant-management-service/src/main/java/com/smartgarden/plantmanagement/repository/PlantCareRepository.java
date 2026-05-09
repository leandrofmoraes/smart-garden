package com.smartgarden.plantmanagement.repository;

import com.smartgarden.plantmanagement.model.PlantCareModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlantCareRepository extends JpaRepository<PlantCareModel, UUID> {
  Optional<PlantCareModel> findByPlantId(UUID plantId);
}
