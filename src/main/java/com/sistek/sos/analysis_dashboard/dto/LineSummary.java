package com.sistek.sos.analysis_dashboard.dto;

/**
 * Hat bilgisi ve barkod adetleri özeti: okunan toplam (quantity) ve ERP aktarım durumuna göre dağılımı
 * (sent = ERP'ye aktarılan, error = aktarılamayan, pending = henüz gönderilmemiş NEW).
 * Hem HTML panosu hem de REST API tarafından ortak kullanılır.
 */
public record LineSummary(
        String id,
        String status,
        long quantity,
        long sent,
        long error,
        long pending
) {}
