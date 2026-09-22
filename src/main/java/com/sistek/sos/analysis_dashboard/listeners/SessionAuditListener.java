package com.sistek.sos.analysis_dashboard.listeners;

import com.sistek.sos.analysis_dashboard.services.AuditService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * HTTP Oturum Olayları Dinleyicisi (T-020, T-020-Eksik).
 * Oturum zaman aşımına uğradığında veya sunucu tarafında geçersiz kılındığında
 * SESSION_EXPIRED denetim kaydı oluşturur (ENG-03 §3.2, ENG-13 §3.1).
 *
 * Kullanıcı kendi isteğiyle çıkış yaptığında (POST /logout), SecurityConfig
 * tarafından oturuma LOGOUT_IN_PROGRESS_ATTR işareti konur; böylece
 * oturum sonlandırılırken mükerrer SESSION_EXPIRED kaydı oluşması engellenir.
 *
 * Kullanıcı adı bilgisi için Tek Doğruluk Kaynağı (ENG-02 §1) oturumdaki
 * Spring Security bağlamıdır (HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY).
 */
@Component
public class SessionAuditListener implements HttpSessionListener {

    public static final String LOGOUT_IN_PROGRESS_ATTR = "LOGOUT_IN_PROGRESS";

    private final AuditService auditService;

    public SessionAuditListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        // Oturum açılmadan sadece sayfayı ziyaret eden anonim kullanıcılara da
        // session atanabileceğinden, tablo şişmesini önlemek için sessionCreated loglanmaz.
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        if (session == null) {
            return;
        }

        try {
            // Açıkça çıkış yapılıyorsa yalnızca LOGOUT kaydı tutulur, SESSION_EXPIRED yazılmaz
            if (Boolean.TRUE.equals(session.getAttribute(LOGOUT_IN_PROGRESS_ATTR))) {
                return;
            }

            // Oturum sahibi kullanıcı adını yalnızca SecurityContext üzerinden tespit et (Tek Doğruluk Kaynağı)
            Object sc = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (sc instanceof SecurityContext context) {
                Authentication auth = context.getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
                    String username = auth.getName();
                    if (username != null && !username.isBlank()) {
                        auditService.record("SESSION_EXPIRED", username, null, null, null);
                    }
                }
            }
        } catch (IllegalStateException ignored) {
            // Oturum servlet konteyneri tarafından zaten tamamen geçersiz kılınmışsa güvenle yoksayılır
        }
    }
}
