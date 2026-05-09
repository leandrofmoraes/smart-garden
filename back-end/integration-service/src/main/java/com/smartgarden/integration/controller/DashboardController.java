package com.smartgarden.integration.controller;

import com.smartgarden.integration.dto.dashboard.DashboardResponseDto;
import com.smartgarden.integration.dto.device.DeviceStatusDto;
import com.smartgarden.integration.dto.messaging.AmqpPlantResponseDto;
import com.smartgarden.integration.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponseDto> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }

    @GetMapping("/status")
    public ResponseEntity<List<DeviceStatusDto>> getStatus() {
        return ResponseEntity.ok(dashboardService.getAllDeviceStatuses());
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<AmqpPlantResponseDto>> getAlerts() {
        return ResponseEntity.ok(dashboardService.getPendingAlerts());
    }
}
