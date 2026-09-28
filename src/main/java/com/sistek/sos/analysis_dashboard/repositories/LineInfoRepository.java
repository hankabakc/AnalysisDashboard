package com.sistek.sos.analysis_dashboard.repositories;

import com.sistek.sos.analysis_dashboard.dto.LineSummary;
import com.sistek.sos.analysis_dashboard.entities.LineInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * LineInfo varlığı için veri erişim katmanı.
 * Hat özeti, seçilen dönemdeki barkod adetleri (toplam + ERP aktarım durumuna göre) ve son barkod zamanı tek sorguda gelir;
 * hat başına ayrı sorgu yok. Son barkod zamanı dönemden bağımsızdır (canlılık göstergesi), bu yüzden dönem koşulu
 * JOIN'de değil sayımların içindedir.
 * Seçilen yedi değer, sırasıyla LineSummary(id, status, quantity, sent, error, pending, lastBarcodeAt) record'una Spring Data tarafından doldurulur.
 */
public interface LineInfoRepository extends JpaRepository<LineInfo, String> {

    /** Barkod seçilen dönemde mi: [from, to) UTC. */
    String IN_PERIOD = "b.creDate >= :from AND b.creDate < :to";

    String SUMMARY = "SELECT l.lineId, l.status,"
            + " count(CASE WHEN " + IN_PERIOD + " THEN 1 END),"
            + " count(CASE WHEN " + IN_PERIOD + " AND b.status = 'SENT' THEN 1 END),"
            + " count(CASE WHEN " + IN_PERIOD + " AND b.status = 'ERROR' THEN 1 END),"
            + " count(CASE WHEN " + IN_PERIOD + " AND b.status = 'NEW' THEN 1 END),"
            + " max(b.creDate)"
            + " FROM LineInfo l LEFT JOIN BarcodeData b ON b.id.lineId = l.lineId ";

    @Query(SUMMARY + """
            GROUP BY l.lineId, l.status
            ORDER BY l.lineId
            """)
    List<LineSummary> findSummaries(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(SUMMARY + """
            WHERE l.lineId = :lineId
            GROUP BY l.lineId, l.status
            """)
    Optional<LineSummary> findSummary(@Param("lineId") String lineId,
                                      @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
