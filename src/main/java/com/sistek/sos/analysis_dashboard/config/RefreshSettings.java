package com.sistek.sos.analysis_dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Pano tazeleme ayarları (application.properties: dashboard.refresh.*).
 * Controller'lar şablona "refresh" adıyla verir; tarayıcıdaki refresh.js değerleri data-* özniteliklerinden okur.
 */
@ConfigurationProperties("dashboard.refresh")
public record RefreshSettings(
        @DefaultValue("5s") Duration interval,
        @DefaultValue("3") int staleAfterIntervals
) {
    public RefreshSettings {
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("dashboard.refresh.interval sıfırdan büyük olmalı: " + interval);
        }
        if (staleAfterIntervals < 2) {
            throw new IllegalArgumentException("dashboard.refresh.stale-after-intervals en az 2 olmalı: " + staleAfterIntervals);
        }
    }

    public long intervalMs() {
        return interval.toMillis();
    }

    /** Son başarılı yanıttan bu kadar süre geçerse ekran "güncel değil" olarak işaretlenir. */
    public long staleMs() {
        return interval.toMillis() * staleAfterIntervals;
    }

    /** İstek zaman aşımı aralıktan kısa tutulur; yoksa yanıt vermeyen sunucuda istekler üst üste biner. */
    public long timeoutMs() {
        return interval.toMillis() * 4 / 5;
    }
}
