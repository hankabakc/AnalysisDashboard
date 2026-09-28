package com.sistek.sos.analysis_dashboard.dto;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Duruş analizinde bir zaman parçası: [from, to) UTC aralığında hattın durumu.
 * widthPercent, parçanın dönemin tamamına oranıdır (zaman çizelgesindeki genişlik).
 */
public record StatusSegment(
        String status,
        LocalDateTime from,
        LocalDateTime to,
        double widthPercent
) {
    public Duration duration() {
        return Duration.between(from, to);
    }

    /** Durum metni; renk tek başına bilgi taşımasın diye çizelgede de yazılır. Tanınmayan/eksik durum "Veri yok". */
    public String label() {
        return switch (status) {
            case "RUN" -> "Çalışıyor";
            case "STOP" -> "Duruş";
            case "PASSIVE" -> "Pasif";
            default -> "Veri yok";
        };
    }

    public String cssClass() {
        return switch (status) {
            case "RUN" -> "bg-success";
            case "STOP" -> "bg-danger";
            case "PASSIVE" -> "bg-secondary";
            default -> "bg-secondary bg-opacity-25";
        };
    }

    public String fromText() {
        return DisplayTime.format(from);
    }

    public String durationText() {
        return DisplayTime.durationPrecise(duration());
    }

    /** Çizelgede üzerine gelince görünen açıklama: "Duruş · 31.07.2025 11:29:00 – 31.07.2025 11:29:18 (18 sn)". */
    public String text() {
        return label() + " · " + fromText() + " – " + DisplayTime.format(to) + " (" + durationText() + ")";
    }
}
