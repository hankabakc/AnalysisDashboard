package com.sistek.sos.analysis_dashboard.dto;

import java.time.LocalDateTime;

/**
 * Seçilen dönem: [from, to) UTC aralığı ve ekrandaki etiketi.
 * "Tüm zamanlar" da geniş, sabit bir aralıktır; böylece sorgularda "dönem yok" dalı gerekmez.
 */
public record Period(
        LocalDateTime from,
        LocalDateTime to,
        String label
) {
    public static final Period ALL = new Period(
            LocalDateTime.of(1970, 1, 1, 0, 0), LocalDateTime.of(9999, 1, 1, 0, 0), "Tüm zamanlar");
}
