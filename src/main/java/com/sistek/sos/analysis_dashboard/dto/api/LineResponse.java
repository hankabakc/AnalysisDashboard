package com.sistek.sos.analysis_dashboard.dto.api;

/**
 * Hat bilgisi ve toplam ürün adedi REST yanıt modeli (T-008).
 */
public record LineResponse(
        String id,
        String status,
        long quantity
) {}
