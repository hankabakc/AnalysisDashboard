package com.sistek.sos.analysis_dashboard.dto;

import java.time.LocalDateTime;

/**
 * Saatlik üretim grafiğinde bir saat: [from, from + 1 sa) UTC aralığında okunan barkodlar ve ERP durumuna göre dağılımı.
 * heightPercent çubuğun en yüksek saate oranıdır (eksen sıfırdan başlar); future, saatin henüz gelmediğini belirtir
 * (gelmemiş saat "0 üretim" gibi gösterilmez).
 */
public record HourBar(
        LocalDateTime from,
        long total,
        long sent,
        long error,
        long pending,
        double heightPercent,
        boolean future
) {
    /** Grafikteki kısa etiket, yerel saat ("11"). */
    public String label() {
        return DisplayTime.time(from).substring(0, 2);
    }

    /** Tablodaki aralık, yerel saat ("11:00–12:00"). */
    public String rangeText() {
        return DisplayTime.time(from) + "–" + DisplayTime.time(from.plusHours(1));
    }
}
