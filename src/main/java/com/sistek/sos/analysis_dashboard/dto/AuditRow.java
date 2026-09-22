package com.sistek.sos.analysis_dashboard.dto;

import com.sistek.sos.analysis_dashboard.entities.AppAuditLog;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Denetim kaydı ekran görünüm modeli (Record).
 * JPA Entity'sinin şablona verilmesini engeller (WEB-07 §1.3).
 * Durum bilgisi yalnız renkle değil, simge ve metinle birlikte taşınır (WEB-04 §2.1).
 */
public record AuditRow(
        Long id,
        String occurredAt,
        String eventLabel,
        String eventIcon,
        String eventBadgeClass,
        String actor,
        String target,
        String oldValue,
        String newValue
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public static AuditRow from(AppAuditLog log) {
        String formattedDate = log.getOccurredAt() != null ? FORMATTER.format(log.getOccurredAt()) : "-";
        String event = log.getEvent() != null ? log.getEvent() : "";

        // Tanınmayan değer (elle eklenmiş ya da eski kayıt) ham adıyla basılır
        String label = event;
        String icon = "ℹ️";
        String badgeClass = "bg-secondary";

        AuditEvent knownEvent = AuditEvent.fromStoredValue(event);
        if (knownEvent != null) {
            switch (knownEvent) {
                case LOGIN_SUCCESS -> {
                    label = "Giriş Başarılı";
                    icon = "🟢";
                    badgeClass = "bg-success";
                }
                case LOGIN_FAILURE -> {
                    label = "Giriş Başarısız";
                    icon = "🔴";
                    badgeClass = "bg-danger";
                }
                case LOGOUT -> {
                    label = "Çıkış Yapıldı";
                    icon = "🚪";
                    badgeClass = "bg-secondary";
                }
                case USER_CREATED -> {
                    label = "Kullanıcı Eklendi";
                    icon = "➕";
                    badgeClass = "bg-primary";
                }
                case USER_UPDATED -> {
                    label = "Kullanıcı Güncellendi";
                    icon = "✏️";
                    badgeClass = "bg-warning text-dark";
                }
                case USER_DELETED -> {
                    label = "Kullanıcı Silindi";
                    icon = "🗑️";
                    badgeClass = "bg-danger";
                }
                case ACCESS_DENIED -> {
                    label = "Yetkisiz Erişim";
                    icon = "⛔";
                    badgeClass = "bg-danger";
                }
                case SESSION_EXPIRED -> {
                    label = "Oturum Süresi Doldu";
                    icon = "⏱️";
                    badgeClass = "bg-secondary";
                }
            }
        }

        return new AuditRow(
                log.getId(),
                formattedDate,
                label,
                icon,
                badgeClass,
                log.getActor() != null ? log.getActor() : "-",
                log.getTarget() != null ? log.getTarget() : "-",
                log.getOldValue(),
                log.getNewValue()
        );
    }
}
