package com.smartgarden.integration.controller;

import com.smartgarden.integration.dto.device.DeviceCommandDto;
import com.smartgarden.integration.dto.device.DeviceStatusDto;
import com.smartgarden.integration.dto.device.IoTDeviceDto;
import com.smartgarden.integration.dto.device.IrrigationReadingDto;
import com.smartgarden.integration.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public ResponseEntity<List<IoTDeviceDto>> listDevices() {
        return ResponseEntity.ok(deviceService.listDevices());
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<IoTDeviceDto> getDevice(@PathVariable String deviceId) {
        return ResponseEntity.ok(deviceService.getDevice(deviceId));
    }

    @GetMapping("/{deviceId}/status")
    public ResponseEntity<DeviceStatusDto> getDeviceStatus(@PathVariable String deviceId) {
        return ResponseEntity.ok(deviceService.getDeviceStatus(deviceId));
    }

    @GetMapping("/{deviceId}/readings")
    public ResponseEntity<List<IrrigationReadingDto>> getDeviceReadings(@PathVariable String deviceId) {
        return ResponseEntity.ok(deviceService.getReadings(deviceId));
    }

    /**
     * FIX: DeviceCommandDto com @Valid substitui Map<String, Object> sem validação.
     * HTTP 202 Accepted: o comando foi aceito e publicado no MQTT.
     */
    @PostMapping("/{deviceId}/commands")
    public ResponseEntity<Void> sendCommand(
            @PathVariable String deviceId,
            @Valid @RequestBody DeviceCommandDto command) {
        deviceService.sendCommand(deviceId, command);
        return ResponseEntity.accepted().build();
    }
}
