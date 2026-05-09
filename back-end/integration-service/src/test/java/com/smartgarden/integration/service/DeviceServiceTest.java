package com.smartgarden.integration.service;

import com.smartgarden.integration.cache.DeviceCache;
import com.smartgarden.integration.cache.ReadingCache;
import com.smartgarden.integration.dto.device.DeviceCommandDto;
import com.smartgarden.integration.dto.device.DeviceStatusDto;
import com.smartgarden.integration.dto.device.IoTDeviceDto;
import com.smartgarden.integration.exception.ResourceNotFoundException;
import com.smartgarden.integration.mqtt.publisher.CommandMqttPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock DeviceCache deviceCache;
    @Mock ReadingCache readingCache;
    @Mock CommandMqttPublisher commandMqttPublisher;

    @InjectMocks DeviceService deviceService;

    @Test
    void getDevice_shouldReturn404WhenNotInCache() {
        when(deviceCache.getDevice("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.getDevice("ghost"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    void getDeviceStatus_shouldReturn404WhenNotInCache() {
        when(deviceCache.getDevice("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.getDeviceStatus("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getDevice_shouldReturnDeviceWhenFound() {
        IoTDeviceDto device = IoTDeviceDto.builder().id("esp-01").name("Sensor").build();
        when(deviceCache.getDevice("esp-01")).thenReturn(Optional.of(device));

        IoTDeviceDto result = deviceService.getDevice("esp-01");
        assertThat(result.getId()).isEqualTo("esp-01");
    }

    @Test
    void sendCommand_shouldPublishToMqtt() {
        when(deviceCache.exists("esp-01")).thenReturn(true);

        DeviceCommandDto cmd = DeviceCommandDto.builder()
                .type("IRRIGATE")
                .build();

        deviceService.sendCommand("esp-01", cmd);

        verify(commandMqttPublisher, times(1))
                .publishCommand(eq("esp-01"), any());
    }

    @Test
    void sendCommand_shouldThrow404ForUnknownDevice() {
        when(deviceCache.exists("ghost")).thenReturn(false);

        DeviceCommandDto cmd = DeviceCommandDto.builder().type("IRRIGATE").build();

        assertThatThrownBy(() -> deviceService.sendCommand("ghost", cmd))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ghost");

        verifyNoInteractions(commandMqttPublisher);
    }

    @Test
    void registerIfAbsent_shouldSkipIfDeviceAlreadyExists() {
        when(deviceCache.exists("esp-01")).thenReturn(true);

        deviceService.registerIfAbsent("esp-01", "192.168.1.1");

        verify(deviceCache, never()).registerDevice(any(), any());
    }

    @Test
    void registerIfAbsent_shouldRegisterNewDevice() {
        when(deviceCache.exists("esp-new")).thenReturn(false);

        deviceService.registerIfAbsent("esp-new", "192.168.1.99");

        verify(deviceCache, times(1)).registerDevice(eq("esp-new"), any());
    }

    @Test
    void getDeviceStatus_shouldReturnOfflineStatusWhenStatusIsNull() {
        IoTDeviceDto device = IoTDeviceDto.builder().id("esp-01").status(null).build();
        when(deviceCache.getDevice("esp-01")).thenReturn(Optional.of(device));

        DeviceStatusDto status = deviceService.getDeviceStatus("esp-01");
        assertThat(status.getDeviceId()).isEqualTo("esp-01");
        assertThat(status.getOnline()).isNull();
    }

    @Test
    void updateStatus_shouldDelegateToCache() {
        DeviceStatusDto status = DeviceStatusDto.builder()
                .deviceId("esp-01").online(true).lastSeen(Instant.now()).build();

        deviceService.updateStatus("esp-01", status);

        verify(deviceCache, times(1)).updateStatus("esp-01", status);
    }
}
