package com.sistek.sos.analysis_dashboard.dto;

import java.time.LocalDateTime;

/**
 * Hat bilgisi ve barkod adetleri özeti: okunan toplam (quantity) ve ERP aktarım durumuna göre dağılımı
 * (sent = ERP'ye aktarılan, error = aktarılamayan, pending = henüz gönderilmemiş NEW), son barkodun zamanı (UTC; barkod yoksa null).
 * Hem HTML panosu hem de REST API tarafından ortak kullanılır.
 */
public record LineSummary(
        String id,
        String status,
        long quantity,
        long sent,
        long error,
        long pending,
        LocalDateTime lastBarcodeAt
) {
    /** Ekran için: son barkod zamanı sunucu saatiyle. */
    public String lastBarcodeText() {
        return DisplayTime.format(lastBarcodeAt);
    }
}
