package com.sistek.sos.analysis_dashboard.repositories;

import com.sistek.sos.analysis_dashboard.entities.BarcodeData;
import com.sistek.sos.analysis_dashboard.entities.BarcodeId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * BarcodeData varlığı için veri erişim katmanı.
 */
public interface BarcodeRepository extends JpaRepository<BarcodeData, BarcodeId> {

    // CAST(... AS text): PostgreSQL null parametrenin tipini bilemediği için gerekli
    @Query(value = """
            SELECT b.barcode AS barcode, b.line_id AS lineId, b.cre_date AS creDate, b.status AS status
            FROM barcode_data b
            WHERE (CAST(:lineId AS text) IS NULL OR b.line_id = CAST(:lineId AS text))
              AND (CAST(:barcodeQuery AS text) IS NULL OR b.barcode ILIKE '%' || CAST(:barcodeQuery AS text) || '%')
              AND (CAST(:status AS text) IS NULL OR b.status = CAST(:status AS text))
            """,
           countQuery = """
            SELECT count(*) FROM barcode_data b
            WHERE (CAST(:lineId AS text) IS NULL OR b.line_id = CAST(:lineId AS text))
              AND (CAST(:barcodeQuery AS text) IS NULL OR b.barcode ILIKE '%' || CAST(:barcodeQuery AS text) || '%')
              AND (CAST(:status AS text) IS NULL OR b.status = CAST(:status AS text))
            """,
           nativeQuery = true)
    Page<BarcodeRowProjection> search(@Param("lineId") String lineId,
                                      @Param("barcodeQuery") String barcodeQuery,
                                      @Param("status") String status,
                                      Pageable pageable);
}
