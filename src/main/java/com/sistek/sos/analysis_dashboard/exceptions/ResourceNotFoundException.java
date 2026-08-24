package com.sistek.sos.analysis_dashboard.exceptions;

/**
 * İstenen kaynak bulunamadığında fırlatılan özel istisna (T-008).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
