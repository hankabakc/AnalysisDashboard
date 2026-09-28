package com.sistek.sos.analysis_dashboard.dto;

import java.util.List;

/**
 * Bir hattın seçilen dönemdeki saatlik üretimi: dönem başından itibaren saat saat kovalar ve en yüksek saatin değeri
 * (grafik ölçeği).
 */
public record HourlyProduction(
        List<HourBar> hours,
        long max
) {}
