package com.sistek.sos.analysis_dashboard.repositories;

import com.sistek.sos.analysis_dashboard.entities.PlcLog;
import com.sistek.sos.analysis_dashboard.entities.PlcLogId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * PlcLog varlığı için veri erişim katmanı.
 */
public interface PlcLogRepository extends JpaRepository<PlcLog, PlcLogId> {

    Page<PlcLog> findByIdPlcId(String plcId, Pageable pageable);
}
