package com.smartgarden.plantmanagement.repository;

import com.smartgarden.plantmanagement.model.PlantModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlantRepository extends JpaRepository<PlantModel, UUID> {

  Optional<PlantModel> findByScientificNameIgnoreCase(String scientificName);

  @Query("SELECT p FROM PlantModel p LEFT JOIN FETCH p.care")
  List<PlantModel> findAllWithCare();

  @Query("SELECT p FROM PlantModel p LEFT JOIN FETCH p.care WHERE p.id = :id")
  Optional<PlantModel> findByIdWithCare(UUID id);
}
