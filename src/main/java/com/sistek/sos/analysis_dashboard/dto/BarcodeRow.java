package com.sistek.sos.analysis_dashboard.dto;

import java.time.LocalDateTime;

/**
 * Tek bir barkod satırı modeli.
 */
public record BarcodeRow(
        String barcode,
        String lineId,
        LocalDateTime creDate,
        String status
) {}
