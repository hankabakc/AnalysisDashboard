package com.sistek.sos.analysis_dashboard.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * İstenen kaynak bulunamadığında fırlatılan özel istisna.
 * API tarafında GlobalApiExceptionHandler ProblemDetail'e çevirir; web tarafında 404 sayfası döner.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
