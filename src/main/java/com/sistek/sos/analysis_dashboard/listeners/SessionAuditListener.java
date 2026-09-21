package com.sistek.sos.analysis_dashboard.listeners;

import com.sistek.sos.analysis_dashboard.services.AuditService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;

/**
 * HTTP Oturum Olayları Dinleyicisi (T-020).
 * Oturum zaman aşımına uğradığında veya sunucu tarafında geçersiz kılındığında
 * SESSION_EXPIRED denetim kaydı oluşturur (ENG-03 §3.2, ENG-13 §3.1).
 *
 * Kullanıcı kendi isteğiyle çıkış yaptığında (POST /logout), SecurityConfig
 * tarafından oturuma LOGOUT_IN_PROGRESS_ATTR işareti konur; böylece
 * oturum sonlandırılırken mükerrer SESSION_EXPIRED kaydı oluşması engellenir.
 */
@Component
public class SessionAuditListener implements HttpSessionListener {

    public static final String LOGOUT_IN_PROGRESS_ATTR = "LOGOUT_IN_PROGRESS";
    public static final String AUTH_USER_ATTR = "AUTH_USER_NAME";

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

            // Oturum sahibi kullanıcı adını tespit et
            String username = (String) session.getAttribute(AUTH_USER_ATTR);

            if (username == null || username.isBlank()) {
                Object sc = session.getAttribute("SPRING_SECURITY_CONTEXT");
                if (sc instanceof SecurityContext context) {
                    Authentication auth = context.getAuthentication();
                    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
                        username = auth.getName();
                    }
                }
            }

            // Yalnızca kimliği doğrulanmış kullanıcıların oturum kapanışları denetime kaydedilir
            if (username != null && !username.isBlank() && !"anonymousUser".equalsIgnoreCase(username)) {
                auditService.record("SESSION_EXPIRED", username, null, null, null);
            }
        } catch (IllegalStateException ignored) {
            // Oturum servlet konteyneri tarafından zaten tamamen geçersiz kılınmışsa güvenle yoksayılır
        }
    }
}
