package com.sistek.sos.analysis_dashboard.dto;

/**
 * Hat bilgisi ve toplam barkod adedi özeti.
 * Hem HTML panosu hem de REST API tarafından ortak kullanılır.
 */
public record LineSummary(
        String id,
        String status,
        long quantity
) {}
