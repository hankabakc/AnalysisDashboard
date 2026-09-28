package com.sistek.sos.analysis_dashboard.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Zaman gösterimi: UTC → sunucu saat dilimi ve süre metni (veritabanı gerekmez).
 */
class DisplayTimeTest {

    @Test
    @DisplayName("format: UTC LocalDateTime ve Instant sunucu saatiyle yazılır (İstanbul = UTC+3); null → '-'")
    void formatConvertsUtcToServerZone() {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"));
            assertEquals("31.07.2025 12:42:46", DisplayTime.format(LocalDateTime.parse("2025-07-31T09:42:46.123")));
            assertEquals("31.07.2025 12:42:46", DisplayTime.format(Instant.parse("2025-07-31T09:42:46Z")));
            assertEquals("-", DisplayTime.format((LocalDateTime) null));
            assertEquals("-", DisplayTime.format((Instant) null));
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    @DisplayName("duration: en büyük birimle yazılır; negatif süre (saat kayması) 0 sn olur")
    void durationUsesLargestUnit() {
        assertEquals("4 sn", DisplayTime.duration(Duration.ofSeconds(4)));
        assertEquals("1 dk", DisplayTime.duration(Duration.ofSeconds(90)));
        assertEquals("13 dk", DisplayTime.duration(Duration.ofMinutes(13)));
        assertEquals("3 sa", DisplayTime.duration(Duration.ofMinutes(200)));
        assertEquals("424 gün", DisplayTime.duration(Duration.ofDays(424).plusHours(5)));
        assertEquals("0 sn", DisplayTime.duration(Duration.ofSeconds(-3)));
    }
}
