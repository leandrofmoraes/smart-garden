package com.smartgarden.plantmanagement.repository;

import com.smartgarden.plantmanagement.model.AlertModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<AlertModel, UUID> {
  List<AlertModel> findByResolvedFalseOrderByCreatedAtDesc();

  List<AlertModel> findByDeviceKeyOrderByCreatedAtDesc(String deviceKey);

  void deleteByPlantId(UUID plantId);
}
