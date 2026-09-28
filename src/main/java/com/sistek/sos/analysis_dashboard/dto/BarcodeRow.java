package com.sistek.sos.analysis_dashboard.dto;

import java.time.LocalDateTime;

/**
 * Tek bir barkod satırı modeli. creDate UTC'dir; error: ERP hata sebebinin Türkçe etiketi, durum ERROR değilse null.
 */
public record BarcodeRow(
        String barcode,
        String lineId,
        LocalDateTime creDate,
        String status,
        String error
) {
    /** Ekran için: okunma zamanı sunucu saatiyle. */
    public String creDateText() {
        return DisplayTime.format(creDate);
    }
}
