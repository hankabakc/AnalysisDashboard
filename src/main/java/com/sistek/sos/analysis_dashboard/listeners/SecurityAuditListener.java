package com.sistek.sos.analysis_dashboard.listeners;

import com.sistek.sos.analysis_dashboard.services.AuditService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Spring Security oturum olayları dinleyicisi (T-017).
 * Başarılı ve başarısız giriş denemelerini yakalayarak denetim kaydına iletir.
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
        Authentication auth = event.getAuthentication();
        if (auth != null && auth.getName() != null && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
            auditService.record("LOGIN_SUCCESS", auth.getName(), null, null, null);
        }
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        Authentication auth = event.getAuthentication();
        String actor = (auth != null && auth.getName() != null) ? auth.getName() : "unknown";
        // Asla parola veya hata türü (kullanıcı yok vs.) yazılmaz, sadece denenen kullanıcı adı actor olarak yazılır
        auditService.record("LOGIN_FAILURE", actor, null, null, null);
    }
}
