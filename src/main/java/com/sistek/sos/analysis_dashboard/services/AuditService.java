package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.dto.AuditEvent;
import com.sistek.sos.analysis_dashboard.dto.AuditRow;
import com.sistek.sos.analysis_dashboard.dto.PageQuery;
import com.sistek.sos.analysis_dashboard.entities.AppAuditLog;
import com.sistek.sos.analysis_dashboard.repositories.AppAuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Denetim kaydı servisi (T-017, T-018, T-021).
 * Denetim kaydı yazımı için TEK YAZMA NOKTASIDIR.
 * Kayıtlar başka hiçbir sınıftan doğrudan veritabanına yazılamaz.
 * Zaman damgası daima UTC saklanır (ENG-03 §1.3).
 */
@Service
@Transactional
public class AuditService {

    private static final int MAX_ACTOR_TARGET_LENGTH = 50;

    /** Kimliği doğrulanmış ve anonim olmayan kullanıcının adını döner (ENG-02 §1). */
    public static String extractAuthenticatedActor(Authentication auth) {
        return (auth != null && auth.isAuthenticated()
                && auth.getName() != null && !auth.getName().isBlank()
                && !"anonymousUser".equalsIgnoreCase(auth.getName())) ? auth.getName() : null;
    }

    private final AppAuditLogRepository auditLogRepository;

    public AuditService(AppAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Yeni bir denetim kaydı ekler.
     *
     * @param event    Olay türü (AuditEvent); veritabanına adı yazılır
     * @param actor    İşlemi gerçekleştiren kullanıcı (veya oturum denemesi yapılan kullanıcı adı)
     * @param target   İşlemden etkilenen kullanıcı (varsa)
     * @param oldValue Değişiklik öncesi durum / eski değer (yalnızca değişen alanlar)
     * @param newValue Değişiklik sonrası durum / yeni değer (yalnızca değişen alanlar)
     */
    public void record(AuditEvent event, String actor, String target, String oldValue, String newValue) {
        AppAuditLog log = new AppAuditLog(
                Instant.now(),
                event.name(),
                truncate(actor, MAX_ACTOR_TARGET_LENGTH),
                truncate(target, MAX_ACTOR_TARGET_LENGTH),
                oldValue,
                newValue
        );
        auditLogRepository.save(log);
    }

    /**
     * Denetim kayıtlarını en yeniden eskiye sıralı ve sayfalanmış olarak döner.
     * Kullanıcı adı (actor veya target) filtresi uygulanabilir.
     */
    @Transactional(readOnly = true)
    public Page<AuditRow> getAuditLogs(String query, PageQuery pageQuery) {
        String trimmedQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        PageRequest pageable = PageRequest.of(
                pageQuery.page(),
                pageQuery.size(),
                Sort.by(Sort.Direction.DESC, "occurredAt", "id")
        );

        Page<AppAuditLog> page = (trimmedQuery == null)
                ? auditLogRepository.findAll(pageable)
                : auditLogRepository.searchLogs(escapeLike(trimmedQuery), pageable);

        return page.map(AuditRow::from);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static String escapeLike(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
