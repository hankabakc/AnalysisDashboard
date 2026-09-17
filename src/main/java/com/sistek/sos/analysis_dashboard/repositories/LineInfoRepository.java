package com.sistek.sos.analysis_dashboard.repositories;

import com.sistek.sos.analysis_dashboard.dto.LineSummary;
import com.sistek.sos.analysis_dashboard.entities.LineInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * LineInfo varlığı için veri erişim katmanı.
 * Hat özeti ve barkod adedi tek sorguda gelir (hat başına ayrı sayım sorgusu yok).
 */
public interface LineInfoRepository extends JpaRepository<LineInfo, String> {

    @Query("""
            SELECT new com.sistek.sos.analysis_dashboard.dto.LineSummary(l.lineId, l.status, count(b.id.barcode))
            FROM LineInfo l LEFT JOIN BarcodeData b ON b.id.lineId = l.lineId
            GROUP BY l.lineId, l.status
            ORDER BY l.lineId
            """)
    List<LineSummary> findSummaries();

    @Query("""
            SELECT new com.sistek.sos.analysis_dashboard.dto.LineSummary(l.lineId, l.status, count(b.id.barcode))
            FROM LineInfo l LEFT JOIN BarcodeData b ON b.id.lineId = l.lineId
            WHERE l.lineId = :lineId
            GROUP BY l.lineId, l.status
            """)
    Optional<LineSummary> findSummary(@Param("lineId") String lineId);
}
