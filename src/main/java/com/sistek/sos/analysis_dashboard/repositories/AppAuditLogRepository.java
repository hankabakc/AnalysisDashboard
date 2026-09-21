package com.sistek.sos.analysis_dashboard.repositories;

import com.sistek.sos.analysis_dashboard.entities.AppAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppAuditLogRepository extends JpaRepository<AppAuditLog, Long> {

    /**
     * Kullanıcı adına göre (actor veya target) filtreli denetim kayıtlarını sayfalar.
     * En yeni kayıtlar en üstte yer alır.
     */
    @Query("SELECT a FROM AppAuditLog a WHERE LOWER(a.actor) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '!' OR LOWER(a.target) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '!'")
    Page<AppAuditLog> searchLogs(@Param("query") String query, Pageable pageable);
}
