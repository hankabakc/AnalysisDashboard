package com.sistek.sos.analysis_dashboard.controllers.api;

import com.sistek.sos.analysis_dashboard.exceptions.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * REST API için merkezi RFC 7807 ProblemDetail hata yönetimi.
 * Yalnızca API controller'larına uygulanır; web sayfalarının hataları templates/error.html ile HTML döner.
 * ResponseEntityExceptionHandler, Spring MVC'nin kendi hatalarını (ör. ?page=abc → 400) da ProblemDetail'e çevirir.
 */
@RestControllerAdvice(basePackages = "com.sistek.sos.analysis_dashboard.controllers.api")
public class GlobalApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Kaynak Bulunamadı");
        return problemDetail;
    }

    // Olmayan kullanıcı da buraya düşer: Spring, UsernameNotFoundException'ı BadCredentialsException'a çevirir
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Kullanıcı adı veya parola hatalı."
        );
        problemDetail.setTitle("Kimlik Doğrulama Başarısız");
        return problemDetail;
    }
}
