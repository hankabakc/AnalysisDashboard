package com.sistek.sos.analysis_dashboard.dto.api;

import java.time.LocalDateTime;

/**
 * Barkod verisi REST yanıt modeli.
 */
public record BarcodeResponse(
        String barcode,
        String lineId,
        LocalDateTime creDate,
        String status
) {}
