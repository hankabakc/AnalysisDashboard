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
 * Hat özeti, barkod adetleri (toplam + ERP aktarım durumuna göre) ve son barkod zamanı tek sorguda gelir; hat başına ayrı sorgu yok.
 * Seçilen yedi değer, sırasıyla LineSummary(id, status, quantity, sent, error, pending, lastBarcodeAt) record'una Spring Data tarafından doldurulur.
 */
public interface LineInfoRepository extends JpaRepository<LineInfo, String> {

    String SUMMARY = """
            SELECT l.lineId, l.status, count(b.id.barcode),
                   count(CASE WHEN b.status = 'SENT' THEN 1 END),
                   count(CASE WHEN b.status = 'ERROR' THEN 1 END),
                   count(CASE WHEN b.status = 'NEW' THEN 1 END),
                   max(b.creDate)
            FROM LineInfo l LEFT JOIN BarcodeData b ON b.id.lineId = l.lineId
            """;

    @Query(SUMMARY + """
            GROUP BY l.lineId, l.status
            ORDER BY l.lineId
            """)
    List<LineSummary> findSummaries();

    @Query(SUMMARY + """
            WHERE l.lineId = :lineId
            GROUP BY l.lineId, l.status
            """)
    Optional<LineSummary> findSummary(@Param("lineId") String lineId);
}
