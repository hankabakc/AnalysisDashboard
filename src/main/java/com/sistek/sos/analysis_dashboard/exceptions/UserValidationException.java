package com.sistek.sos.analysis_dashboard.exceptions;

import java.util.Collections;
import java.util.Map;

/**
 * Kullanıcı yönetimi form doğrulama ve iş kuralı hatası (Domain Exception).
 * Alan bazlı ("username", "password", "roles") ve genel ("general") hata mesajlarını taşır.
 */
public class UserValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public UserValidationException(Map<String, String> fieldErrors) {
        super("Form doğrulama hatası: " + fieldErrors);
        this.fieldErrors = (fieldErrors != null) ? Map.copyOf(fieldErrors) : Collections.emptyMap();
    }

    public UserValidationException(String generalMessage) {
        super(generalMessage);
        this.fieldErrors = (generalMessage != null) ? Map.of("general", generalMessage) : Collections.emptyMap();
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
