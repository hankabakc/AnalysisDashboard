package com.sistek.sos.analysis_dashboard.repositories;

import com.sistek.sos.analysis_dashboard.entities.BarcodeData;
import com.sistek.sos.analysis_dashboard.entities.BarcodeId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * BarcodeData varlığı için veri erişim katmanı (SRP & Repository Pattern).
 */
@Repository
public interface BarcodeRepository extends JpaRepository<BarcodeData, BarcodeId> {

    @Query(value = """
            SELECT b.line_id AS lineId, count(b.barcode) AS quantity
            FROM barcode_data b
            GROUP BY b.line_id
            """, nativeQuery = true)
    List<LineBarcodeCountProjection> countBarcodesGroupedByLineId();

    @Query(value = """
            SELECT b.barcode AS barcode, b.cre_date AS creDate, b.status AS status
            FROM barcode_data b
            WHERE b.line_id = :lineId
              AND (CAST(:barcodeQuery AS text) IS NULL OR b.barcode ILIKE '%' || CAST(:barcodeQuery AS text) || '%')
              AND (CAST(:status AS text) IS NULL OR b.status = CAST(:status AS text))
            """,
           countQuery = """
            SELECT count(*) FROM barcode_data b
            WHERE b.line_id = :lineId
              AND (CAST(:barcodeQuery AS text) IS NULL OR b.barcode ILIKE '%' || CAST(:barcodeQuery AS text) || '%')
              AND (CAST(:status AS text) IS NULL OR b.status = CAST(:status AS text))
            """,
           nativeQuery = true)
    Page<BarcodeRowProjection> search(@Param("lineId") String lineId,
                                      @Param("barcodeQuery") String barcodeQuery,
                                      @Param("status") String status,
                                      Pageable pageable);
}
