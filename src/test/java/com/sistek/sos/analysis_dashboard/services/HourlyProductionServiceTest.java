package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.dto.HourBar;
import com.sistek.sos.analysis_dashboard.dto.HourlyProduction;
import com.sistek.sos.analysis_dashboard.services.HourlyProductionService.HourCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Saatlik kova hesabı (veritabanı gerekmez). Dönem 8 saatlik vardiya: [05:00, 13:00) UTC = yerel 08:00–16:00
 * (surefire argLine: Europe/Istanbul).
 */
class HourlyProductionServiceTest {

    private static final LocalDateTime FROM = LocalDateTime.parse("2025-07-31T05:00");
    private static final LocalDateTime TO = FROM.plusHours(8);

    @Test
    @DisplayName("8 kova; eksik saat 0; yükseklik en yüksek saate göre; 12:43'te 13:00 sonrası 'henüz gelmedi'")
    void bucketsAShift() {
        HourlyProduction hourly = HourlyProductionService.buckets(FROM, TO, FROM.plusHours(4).plusMinutes(43),
                List.of(new HourCount(3, 85, 0, 85, 0), new HourCount(4, 112, 0, 112, 0)));

        List<HourBar> hours = hourly.hours();
        assertEquals(8, hours.size());
        assertEquals(112, hourly.max());
        assertEquals("08", hours.get(0).label());
        assertEquals("11:00–12:00", hours.get(3).rangeText());
        assertEquals(0, hours.get(0).total());
        assertFalse(hours.get(0).future());
        assertEquals(85, hours.get(3).total());
        assertEquals(100.0 * 85 / 112, hours.get(3).heightPercent(), 1e-9);
        assertEquals(100.0, hours.get(4).heightPercent(), 1e-9);
        assertFalse(hours.get(4).future(), "içinde bulunulan saat gelmiş sayılır");
        assertTrue(hours.get(5).future());
        assertTrue(hours.get(7).future());
    }

    @Test
    @DisplayName("Hiç barkod yoksa bütün yükseklikler 0 (sıfıra bölme yok); tam gün 24 kova")
    void emptyDay() {
        HourlyProduction hourly = HourlyProductionService.buckets(FROM, FROM.plusDays(1), FROM.plusDays(2), List.of());
        assertEquals(24, hourly.hours().size());
        assertEquals(0, hourly.max());
        assertTrue(hourly.hours().stream().allMatch(hour -> hour.heightPercent() == 0 && !hour.future()));
    }
}
