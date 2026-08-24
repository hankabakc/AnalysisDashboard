package com.sistek.sos.analysis_dashboard.exceptions;

/**
 * İstenen kaynak bulunamadığında fırlatılan özel istisna.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
