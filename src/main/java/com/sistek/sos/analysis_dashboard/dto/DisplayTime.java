package com.sistek.sos.analysis_dashboard.dto;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Ekrandaki zaman gösterimi tek yerde.
 * Veritabanındaki saatler UTC'dir (S-003); ekranda sunucunun saat diliminde gösterilir.
 * Sunucu fabrikanın saat dilimine (Europe/Istanbul) ayarlanır; konteynerde TZ ortam değişkeniyle.
 */
public final class DisplayTime {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private DisplayTime() {
    }

    /** Veritabanından gelen saat dilimsiz UTC zamanı sunucu saatiyle yazar; null ise "-". */
    public static String format(LocalDateTime utc) {
        return utc == null ? "-" : format(utc.toInstant(ZoneOffset.UTC));
    }

    public static String format(Instant instant) {
        return instant == null ? "-" : FORMAT.format(instant.atZone(ZoneId.systemDefault()));
    }

    /** Süreyi en büyük birimle yazar: "12 sn", "5 dk", "3 sa", "2 gün". Negatif süre (saat kayması) 0 sayılır. */
    public static String duration(Duration duration) {
        long seconds = Math.max(0, duration.toSeconds());
        if (seconds < 60) {
            return seconds + " sn";
        }
        if (seconds < 3600) {
            return seconds / 60 + " dk";
        }
        if (seconds < 86400) {
            return seconds / 3600 + " sa";
        }
        return seconds / 86400 + " gün";
    }
}
