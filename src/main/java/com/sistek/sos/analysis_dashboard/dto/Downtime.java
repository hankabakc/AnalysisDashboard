package com.sistek.sos.analysis_dashboard.dto;

import java.time.Duration;
import java.util.List;

/**
 * Bir hattın seçilen dönemdeki çalışma/duruş analizi. Süreler dönemin geçmiş kısmı içindir (süren vardiyada şimdiye kadar).
 *
 * @param segments     zaman çizelgesi, sıralı ve ardışık aynı durumlar birleşmiş
 * @param stopCount    dönemde görülen duruş sayısı (dönemden önce başlayıp süren duruş dahil)
 * @param availability çalışma / (çalışma + duruş), yüzde; PASİF (planlı) paydaya girmez; hesaplanacak süre yoksa null
 * @param longestStops en uzun 5 duruş, uzundan kısaya
 */
public record Downtime(
        List<StatusSegment> segments,
        Duration run,
        Duration stop,
        int stopCount,
        Duration longestStop,
        Integer availability,
        List<StatusSegment> longestStops
) {
    public String runText() {
        return DisplayTime.durationPrecise(run);
    }

    public String stopText() {
        return DisplayTime.durationPrecise(stop);
    }

    public String longestStopText() {
        return DisplayTime.durationPrecise(longestStop);
    }
}
