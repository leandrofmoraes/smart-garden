package com.smartgarden.integration.service;

import com.smartgarden.integration.cache.ReadingCache;
import com.smartgarden.integration.dto.device.IrrigationReadingDto;
import com.smartgarden.integration.messaging.publisher.ReadingAmqpPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingServiceTest {

    @Mock ReadingCache readingCache;
    @Mock ReadingAmqpPublisher readingAmqpPublisher;

    @InjectMocks ReadingService readingService;

    @Test
    void process_shouldCacheAndPublish() {
        IrrigationReadingDto reading = IrrigationReadingDto.builder()
                .deviceId("esp-01").humidity(65.0).timestamp(Instant.now()).build();

        readingService.process(reading);

        verify(readingCache, times(1)).put("esp-01", reading);
        verify(readingAmqpPublisher, times(1)).publish(reading);
    }

    @Test
    void getReadingsForDevice_shouldDelegateToCache() {
        IrrigationReadingDto r = IrrigationReadingDto.builder()
                .deviceId("esp-01").humidity(50.0).build();
        when(readingCache.getReadingsForDevice("esp-01")).thenReturn(List.of(r));

        List<IrrigationReadingDto> result = readingService.getReadingsForDevice("esp-01");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHumidity()).isEqualTo(50.0);
    }

    @Test
    void getLatestPerDevice_shouldDelegateToCache() {
        when(readingCache.getLatestPerDevice()).thenReturn(List.of(
                IrrigationReadingDto.builder().deviceId("esp-01").build(),
                IrrigationReadingDto.builder().deviceId("esp-02").build()
        ));

        assertThat(readingService.getLatestPerDevice()).hasSize(2);
    }
}
