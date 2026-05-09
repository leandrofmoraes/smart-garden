package com.smartgarden.integration.cache;

import com.smartgarden.integration.dto.device.DeviceStatusDto;
import com.smartgarden.integration.dto.device.IoTDeviceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceCacheTest {

    private DeviceCache cache;

    @BeforeEach
    void setUp() {
        cache = new DeviceCache();
    }

    @Test
    void shouldRegisterAndRetrieveDevice() {
        cache.registerDevice("esp-01", device("esp-01", "Sensor Jardim"));
        var found = cache.getDevice("esp-01");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Sensor Jardim");
    }

    @Test
    void shouldReturnEmptyForUnknownDevice() {
        assertThat(cache.getDevice("nonexistent")).isEmpty();
    }

    @Test
    void existsShouldReflectCacheState() {
        assertThat(cache.exists("esp-01")).isFalse();
        cache.registerDevice("esp-01", device("esp-01", "Test"));
        assertThat(cache.exists("esp-01")).isTrue();
    }

    @Test
    void shouldUpdateStatusForExistingDevice() {
        cache.registerDevice("esp-02", device("esp-02", "Sensor Varanda"));

        DeviceStatusDto status = DeviceStatusDto.builder()
                .deviceId("esp-02").ip("192.168.1.11").rssi(-55)
                .online(true).lastSeen(Instant.now()).build();
        cache.updateStatus("esp-02", status);

        var updated = cache.getDevice("esp-02");
        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus().getRssi()).isEqualTo(-55);
        assertThat(updated.get().getStatus().getOnline()).isTrue();
    }

    @Test
    void shouldAutoCreateDeviceOnStatusUpdateIfAbsent() {
        DeviceStatusDto status = DeviceStatusDto.builder()
                .deviceId("esp-new").ip("192.168.1.20")
                .online(true).lastSeen(Instant.now()).build();
        cache.updateStatus("esp-new", status);

        assertThat(cache.getDevice("esp-new")).isPresent();
        assertThat(cache.exists("esp-new")).isTrue();
    }

    @Test
    void shouldListAllDevices() {
        cache.registerDevice("d1", device("d1", "D1"));
        cache.registerDevice("d2", device("d2", "D2"));
        assertThat(cache.getAllDevices()).hasSize(2);
    }

    private IoTDeviceDto device(String id, String name) {
        return IoTDeviceDto.builder().id(id).name(name).build();
    }
}
