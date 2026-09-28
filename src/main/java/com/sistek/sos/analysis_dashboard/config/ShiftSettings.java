package com.sistek.sos.analysis_dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

/**
 * Vardiya başlangıç saatleri, fabrikanın yerel saatiyle (application.properties: dashboard.shift.starts).
 * İlk saat üretim gününün başıdır; sonrakiler üretim günü içindeki sırayla verilir. Gece yarısını geçen vardiya
 * ertesi takvim gününe düşer (08:00,16:00,00:00 → 3. vardiya ertesi gün 00:00–08:00).
 * Her vardiya bir sonrakinin başında, sonuncusu ertesi üretim gününün başında biter.
 * HH:mm deseni açıkça verilir: varsayılan ayrıştırma sunucunun yerel ayarına göre değişir.
 */
@ConfigurationProperties("dashboard.shift")
public record ShiftSettings(
        @DefaultValue({"08:00", "16:00", "00:00"}) @DateTimeFormat(pattern = "HH:mm") List<LocalTime> starts
) {
    public ShiftSettings {
        if (starts == null || starts.isEmpty()) {
            throw new IllegalArgumentException("dashboard.shift.starts en az bir vardiya içermeli");
        }
        for (int i = 1; i < starts.size(); i++) {
            if (offset(starts, i).compareTo(offset(starts, i - 1)) <= 0) {
                throw new IllegalArgumentException("dashboard.shift.starts üretim günü içinde ileri sırayla verilmeli: " + starts);
            }
        }
        starts = List.copyOf(starts);
    }

    public int count() {
        return starts.size();
    }

    /** 0'dan sayılan vardiyanın üretim günü başından itibaren başlangıcı; index == count() ertesi üretim gününün başıdır (24 sa). */
    public Duration offset(int index) {
        return index == starts.size() ? Duration.ofDays(1) : offset(starts, index);
    }

    private static Duration offset(List<LocalTime> starts, int index) {
        Duration sinceDayStart = Duration.between(starts.get(0), starts.get(index));
        return sinceDayStart.isNegative() ? sinceDayStart.plusDays(1) : sinceDayStart;
    }
}
