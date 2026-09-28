package com.sistek.sos.analysis_dashboard.dto;

import java.time.LocalDateTime;

/**
 * Tek bir barkod satırı modeli. error: ERP hata sebebinin Türkçe etiketi, durum ERROR değilse null.
 */
public record BarcodeRow(
        String barcode,
        String lineId,
        LocalDateTime creDate,
        String status,
        String error
) {}
