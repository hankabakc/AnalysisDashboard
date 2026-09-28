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

    @Test
    @DisplayName("durationPrecise: en büyük iki birim, sıfır olan ikinci birim yazılmaz")
    void durationPreciseUsesTwoUnits() {
        assertEquals("26 sn", DisplayTime.durationPrecise(Duration.ofMillis(26_008)));
        assertEquals("4 dk 2 sn", DisplayTime.durationPrecise(Duration.ofSeconds(242)));
        assertEquals("5 dk", DisplayTime.durationPrecise(Duration.ofMinutes(5)));
        assertEquals("1 sa 12 dk", DisplayTime.durationPrecise(Duration.ofSeconds(4340)));
        assertEquals("3 sa", DisplayTime.durationPrecise(Duration.ofHours(3)));
        assertEquals("1 gün 5 sa", DisplayTime.durationPrecise(Duration.ofHours(29).plusMinutes(10)));
        assertEquals("0 sn", DisplayTime.durationPrecise(Duration.ZERO));
    }
}
