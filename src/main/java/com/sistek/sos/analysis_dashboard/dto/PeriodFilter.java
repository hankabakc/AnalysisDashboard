package com.sistek.sos.analysis_dashboard.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Set;

/**
 * Dönem seçimi: ?range=shift|today|all (göreli, varsayılan shift) veya ?day=2025-07-28[&shift=2] (mutlak).
 * Göreli dönem her istekte yeniden hesaplanır; açık kalan ekran vardiya değişince kendiliğinden kayar.
 * day verilirse range yok sayılır. Bilinmeyen range varsayılana düşer; gün yoksa vardiya numarası atılır.
 * Bozuk tarih (?day=abc) bağlamada 400 verir.
 */
public record PeriodFilter(
        String range,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
        Integer shift
) {
    private static final Set<String> RANGES = Set.of("shift", "today", "all");

    public PeriodFilter {
        range = (range != null && RANGES.contains(range)) ? range : "shift";
        if (day == null) {
            shift = null;
        }
    }
}
