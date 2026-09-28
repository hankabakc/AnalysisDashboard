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
 * hat başına ayrı sorgu yok. Dönem koşulu JOIN'dedir: yalnızca dönemin barkodları okunur, (line_id, cre_date) indeksiyle.
 * Son barkod zamanı dönemden bağımsızdır (canlılık göstergesi); hat başına alt sorgu olarak aynı indeksten tek satırla okunur.
 * (Önceden dönem koşulu sayımların içindeydi ve her tazeleme tablonun tamamını birleştiriyordu: 1 M barkodda ~490 ms.)
 * Seçilen yedi değer, sırasıyla LineSummary(id, status, quantity, sent, error, pending, lastBarcodeAt) record'una Spring Data tarafından doldurulur.
 */
public interface LineInfoRepository extends JpaRepository<LineInfo, String> {

    String SUMMARY = "SELECT l.lineId, l.status,"
            + " count(b.creDate),"
            + " count(CASE WHEN b.status = 'SENT' THEN 1 END),"
            + " count(CASE WHEN b.status = 'ERROR' THEN 1 END),"
            + " count(CASE WHEN b.status = 'NEW' THEN 1 END),"
            + " (SELECT max(last.creDate) FROM BarcodeData last WHERE last.id.lineId = l.lineId)"
            + " FROM LineInfo l LEFT JOIN BarcodeData b"
            + " ON b.id.lineId = l.lineId AND b.creDate >= :from AND b.creDate < :to ";

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
