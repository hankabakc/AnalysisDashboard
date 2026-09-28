package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.config.ShiftSettings;
import com.sistek.sos.analysis_dashboard.dto.Period;
import com.sistek.sos.analysis_dashboard.dto.PeriodFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Dönem → UTC aralığı hesabı (veritabanı gerekmez). Vardiyalar 08-16, 16-00, 00-08; sunucu saati Europe/Istanbul (UTC+3,
 * surefire argLine). Beklenen from/to değerleri UTC'dir.
 */
class PeriodServiceTest {

    private static final ShiftSettings SHIFTS =
            new ShiftSettings(List.of(LocalTime.of(8, 0), LocalTime.of(16, 0), LocalTime.of(0, 0)));

    private static PeriodService at(String utcInstant) {
        return new PeriodService(SHIFTS, Clock.fixed(Instant.parse(utcInstant), ZoneOffset.UTC));
    }

    private static void assertRange(String fromUtc, String toUtc, Period period) {
        assertEquals(LocalDateTime.parse(fromUtc), period.from());
        assertEquals(LocalDateTime.parse(toUtc), period.to());
    }

    @Test
    @DisplayName("Varsayılan = bu vardiya: 12:43 yerel → 31.07 1. vardiya, UTC 05:00–13:00")
    void defaultIsCurrentShift() {
        Period period = at("2025-07-31T09:43:00Z").resolve(new PeriodFilter(null, null, null));
        assertRange("2025-07-31T05:00", "2025-07-31T13:00", period);
        assertEquals("Bu vardiya: 31.07.2025 · 1. vardiya (08:00–16:00)", period.label());
    }

    @Test
    @DisplayName("Gece 01:30 yerel: üretim günü önceki gün, 3. vardiya; 'bugün' de önceki üretim günüdür")
    void nightBelongsToPreviousProductionDay() {
        PeriodService service = at("2025-07-31T22:30:00Z");
        Period shift = service.resolve(new PeriodFilter("shift", null, null));
        assertRange("2025-07-31T21:00", "2025-08-01T05:00", shift);
        assertEquals("Bu vardiya: 31.07.2025 · 3. vardiya (00:00–08:00)", shift.label());

        Period today = service.resolve(new PeriodFilter("today", null, null));
        assertRange("2025-07-31T05:00", "2025-08-01T05:00", today);
        assertEquals("Bugün: 31.07.2025 · tüm gün (08:00–08:00)", today.label());
    }

    @Test
    @DisplayName("Sınır: 08:00:00 yerel yeni vardiyanın ilk anıdır, 07:59:59 önceki günün 3. vardiyasıdır")
    void shiftBoundaryBelongsToNewShift() {
        assertRange("2025-07-31T05:00", "2025-07-31T13:00", at("2025-07-31T05:00:00Z").resolve(new PeriodFilter("shift", null, null)));
        assertRange("2025-07-30T21:00", "2025-07-31T05:00", at("2025-07-31T04:59:59Z").resolve(new PeriodFilter("shift", null, null)));
    }

    @Test
    @DisplayName("Belirli gün: day + shift o vardiya; geçersiz vardiya numarası ya da vardiyasız gün tüm gündür; range yok sayılır")
    void specificDay() {
        PeriodService service = at("2025-07-31T09:43:00Z");
        LocalDate day = LocalDate.of(2025, 7, 28);

        Period second = service.resolve(new PeriodFilter("all", day, 2));
        assertRange("2025-07-28T13:00", "2025-07-28T21:00", second);
        assertEquals("28.07.2025 · 2. vardiya (16:00–00:00)", second.label());

        assertRange("2025-07-28T05:00", "2025-07-29T05:00", service.resolve(new PeriodFilter(null, day, 7)));
        assertRange("2025-07-28T05:00", "2025-07-29T05:00", service.resolve(new PeriodFilter(null, day, null)));
    }

    @Test
    @DisplayName("range=all: tüm zamanlar sabit geniş aralıktır")
    void allTime() {
        assertEquals(Period.ALL, at("2025-07-31T09:43:00Z").resolve(new PeriodFilter("all", null, null)));
    }

    @Test
    @DisplayName("PeriodFilter: bilinmeyen range 'shift' olur; gün yoksa vardiya numarası atılır")
    void filterNormalizes() {
        PeriodFilter filter = new PeriodFilter("HACK", null, 2);
        assertEquals("shift", filter.range());
        assertNull(filter.shift());
    }

    @Test
    @DisplayName("Vardiya seçenek etiketleri ve ayar doğrulaması: sıra üretim günü içinde ileri gitmeli, liste boş olamaz")
    void shiftLabelsAndSettingsValidation() {
        assertEquals(List.of("1. vardiya (08:00–16:00)", "2. vardiya (16:00–00:00)", "3. vardiya (00:00–08:00)"),
                at("2025-07-31T09:43:00Z").shiftLabels());
        assertThrows(IllegalArgumentException.class,
                () -> new ShiftSettings(List.of(LocalTime.of(8, 0), LocalTime.of(0, 0), LocalTime.of(16, 0))));
        assertThrows(IllegalArgumentException.class, () -> new ShiftSettings(List.of()));
    }
}
