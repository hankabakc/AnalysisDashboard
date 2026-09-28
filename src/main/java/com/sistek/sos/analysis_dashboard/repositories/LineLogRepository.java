package com.sistek.sos.analysis_dashboard.repositories;

import com.sistek.sos.analysis_dashboard.entities.LineLog;
import com.sistek.sos.analysis_dashboard.entities.LineLogId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * LineLog varlığı için veri erişim katmanı.
 * Duruş analizi sorgularında lineId null ise tüm hatlar döner.
 */
public interface LineLogRepository extends JpaRepository<LineLog, LineLogId> {

    Page<LineLog> findByIdLineId(String lineId, Pageable pageable);

    /** Her hattın verilen UTC zamandan önceki son durumu, yani dönem başındaki durumu (hat başına tek satır). */
    @Query(value = """
            SELECT DISTINCT ON (line_id) line_id AS lineId, proc_date AS procDate, status
            FROM line_log
            WHERE proc_date < :before
              AND (CAST(:lineId AS text) IS NULL OR line_id = CAST(:lineId AS text))
            ORDER BY line_id, proc_date DESC, seq_no DESC
            """, nativeQuery = true)
    List<LineStatusProjection> findLastStatusBefore(@Param("before") LocalDateTime before, @Param("lineId") String lineId);

    /** [from, to) UTC aralığındaki durum değişimleri, hat ve zaman sırasıyla. Dönem en fazla 1 gün olduğu için sınırlıdır. */
    @Query(value = """
            SELECT line_id AS lineId, proc_date AS procDate, status
            FROM line_log
            WHERE proc_date >= :from AND proc_date < :to
              AND (CAST(:lineId AS text) IS NULL OR line_id = CAST(:lineId AS text))
            ORDER BY line_id, proc_date, seq_no
            """, nativeQuery = true)
    List<LineStatusProjection> findStatusChanges(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                                 @Param("lineId") String lineId);
}
