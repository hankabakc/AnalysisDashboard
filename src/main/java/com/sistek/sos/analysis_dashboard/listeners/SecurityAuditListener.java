package com.sistek.sos.analysis_dashboard.listeners;

import com.sistek.sos.analysis_dashboard.services.AuditService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Spring Security oturum olayları dinleyicisi (T-017, T-017-Eksik, T-020, T-020-Eksik).
 * Yalnızca form login ve API giriş denemelerini (UsernamePasswordAuthenticationToken) yakalar.
 * Her API isteğindeki token doğrulaması (JwtAuthenticationToken) denetim kaydına dönüştürülmez.
 * Parola bilgisi HİÇBİR ZAMAN kaydedilmez (ENG-03 §3.3).
 * Kullanıcı var/yok ayrımı kayda yansıtılmaz (ENG-11 §1.2).
 */
@Component
public class SecurityAuditListener {

    private final AuditService auditService;

    public SecurityAuditListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication() instanceof UsernamePasswordAuthenticationToken auth) {
            String actor = AuditService.extractAuthenticatedActor(auth);
            if (actor != null) {
                auditService.record("LOGIN_SUCCESS", actor, null, null, null);
            }
        }
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        Authentication auth = event.getAuthentication();
        if (auth instanceof UsernamePasswordAuthenticationToken) {
            String actor = (auth.getName() != null) ? auth.getName() : "unknown";
            // Asla parola veya hata türü (kullanıcı yok vs.) yazılmaz, sadece denenen kullanıcı adı actor olarak yazılır
            auditService.record("LOGIN_FAILURE", actor, null, null, null);
        }
    }
}
