package com.sistek.sos.analysis_dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Kaynak sistem (PLC toplayıcı) sağlık eşikleri (application.properties: dashboard.source.*).
 * Toplayıcı plc_info.last_status_date'i ~5 sn'de bir günceller; eşikler sahada bu sıklığa göre ayarlanır.
 *
 * @param silentAfter son sinyal bundan eskiyse toplayıcı "veri yazmıyor" sayılır
 * @param stuckAfter  NEW durumundaki barkod bundan uzun süredir bekliyorsa ERP'ye gönderimi takılmış sayılır
 */
@ConfigurationProperties("dashboard.source")
public record SourceSettings(
        @DefaultValue("60s") Duration silentAfter,
        @DefaultValue("5m") Duration stuckAfter
) {
    public SourceSettings {
        if (silentAfter.isNegative() || silentAfter.isZero() || stuckAfter.isNegative() || stuckAfter.isZero()) {
            throw new IllegalArgumentException("dashboard.source eşikleri sıfırdan büyük olmalı: " + silentAfter + ", " + stuckAfter);
        }
    }
}
