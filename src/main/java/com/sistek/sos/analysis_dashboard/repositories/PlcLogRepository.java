package com.sistek.sos.analysis_dashboard.repositories;

import com.sistek.sos.analysis_dashboard.entities.PlcLog;
import com.sistek.sos.analysis_dashboard.entities.PlcLogId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PlcLog varlığı için veri erişim katmanı.
 */
public interface PlcLogRepository extends JpaRepository<PlcLog, PlcLogId> {

    Page<PlcLog> findByIdPlcId(String plcId, Pageable pageable);

    /**
     * Toplayıcının sustuğu aralıklar. plc_log'daki her satır bir kalp atışıdır ve kendinden sonraki silentSeconds kadar
     * süreyi kapsar; iki atış arası bundan uzunsa [önceki atış + eşik, sonraki atış) aralığında toplayıcı sustu demektir.
     * Pencerenin başına (windowStart) ve sonuna (windowEnd) birer sanal atış eklenir: böylece baştaki, aradaki ve
     * sondaki boşluklar aynı kuralla bulunur. Veritabanından atışlar değil yalnızca boşluklar döner.
     */
    @Query(value = """
            SELECT previous_beat + make_interval(secs => CAST(:silentSeconds AS double precision)) AS silentFrom,
                   beat AS silentTo
            FROM (SELECT beat, lag(beat) OVER (ORDER BY beat) AS previous_beat
                  FROM (SELECT proc_date AS beat FROM plc_log WHERE proc_date >= :windowStart AND proc_date < :beatsUntil
                        UNION ALL SELECT CAST(:windowStart AS timestamp)
                        UNION ALL SELECT CAST(:windowEnd AS timestamp)) beats) pairs
            WHERE beat - previous_beat > make_interval(secs => CAST(:silentSeconds AS double precision))
            ORDER BY beat
            """, nativeQuery = true)
    List<SilenceProjection> findCollectorSilences(@Param("windowStart") LocalDateTime windowStart,
                                                  @Param("beatsUntil") LocalDateTime beatsUntil,
                                                  @Param("windowEnd") LocalDateTime windowEnd,
                                                  @Param("silentSeconds") long silentSeconds);
}
