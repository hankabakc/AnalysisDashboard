package com.sistek.sos.analysis_dashboard.repositories;

import com.sistek.sos.analysis_dashboard.entities.LineLog;
import com.sistek.sos.analysis_dashboard.entities.LineLogId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * LineLog varlığı için veri erişim katmanı.
 */
@Repository
public interface LineLogRepository extends JpaRepository<LineLog, LineLogId> {

    Page<LineLog> findByIdLineId(String lineId, Pageable pageable);
}
