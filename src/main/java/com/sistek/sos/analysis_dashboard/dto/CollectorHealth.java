package com.sistek.sos.analysis_dashboard.dto;

/**
 * Kaynak sistemin (PLC toplayıcı) sağlığı, panoda gösterilmeye hazır.
 *
 * @param lastSignal PLC'den son sinyalin zamanı (sunucu saatiyle)
 * @param signalAge  son sinyalden bu yana geçen süre ("3 sn", "13 dk")
 * @param silent     sinyal eşiği aşıldı mı; aşıldıysa ekrandaki durumlar yalnızca son bilinen değerlerdir
 * @param stuckCount eşikten uzun süredir NEW durumunda, yani ERP'ye gönderilmeyi bekleyen barkod sayısı
 * @param stuckAfter o eşik ("5 dk")
 */
public record CollectorHealth(
        String lastSignal,
        String signalAge,
        boolean silent,
        long stuckCount,
        String stuckAfter
) {}
