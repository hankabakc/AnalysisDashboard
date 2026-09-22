package com.sistek.sos.analysis_dashboard.dto;

/**
 * Denetim kaydına yazılabilen olaylar.
 * `app_audit_log.event` sütunu bu sabitlerin adını saklar; yazan taraf düz metin kullanmaz.
 */
public enum AuditEvent {

    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    ACCESS_DENIED,
    SESSION_EXPIRED;

    /** Veritabanındaki metni olaya çevirir; tanınmayan değer (ör. elle eklenmiş eski kayıt) için null döner. */
    public static AuditEvent fromStoredValue(String value) {
        for (AuditEvent event : values()) {
            if (event.name().equals(value)) {
                return event;
            }
        }
        return null;
    }
}
