package com.sistek.sos.analysis_dashboard.exceptions;

/**
 * Kullanıcı yönetimi iş kuralı ve kilitlenme koruması hatası (Domain Exception).
 */
public class UserBusinessException extends RuntimeException {

    public UserBusinessException(String message) {
        super(message);
    }
}
