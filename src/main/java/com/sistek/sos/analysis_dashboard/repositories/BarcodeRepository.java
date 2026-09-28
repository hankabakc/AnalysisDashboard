package com.sistek.sos.analysis_dashboard.repositories;

import com.sistek.sos.analysis_dashboard.entities.BarcodeData;
import com.sistek.sos.analysis_dashboard.entities.BarcodeId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * BarcodeData varlığı için veri erişim katmanı.
 */
public interface BarcodeRepository extends JpaRepository<BarcodeData, BarcodeId> {

    /**
     * api_result metninden ERP hata kodu: "ORA-20100" gibi Oracle kodu, ERP'ye hiç ulaşılamadıysa "CONNECTION",
     * tanınmayan metinde null. substring(metin, desen) PostgreSQL'in düzenli ifade sürümüdür.
     * Metin JSON olarak ayrıştırılmaz: kaynak sistem bağlantı hatalarını düz metin yazıyor ve tek bozuk satır
     * ::jsonb dönüşümünü, dolayısıyla tüm sorguyu düşürürdü.
     */
    String ERROR_CODE = """
            COALESCE(substring(b.api_result, 'ORA-[0-9]+'),
                     CASE WHEN b.api_result LIKE 'Failed to send%' THEN 'CONNECTION' END)""";

    /**
     * Dinamik barkod filtresi: Gönderilen parametreler null ise filtre uygulanmaz.
     * CAST(... AS text): PostgreSQL'in null parametrelerde veri tipini belirleyebilmesi için gereklidir.
     * ILIKE: Barkod metninde büyük/küçük harf duyarsız arama yapar.
     */
    String SEARCH_FILTER = """
            FROM barcode_data b
            WHERE (CAST(:lineId AS text) IS NULL OR b.line_id = CAST(:lineId AS text))
              AND (CAST(:barcodeQuery AS text) IS NULL OR b.barcode ILIKE '%' || CAST(:barcodeQuery AS text) || '%' ESCAPE '!')
              AND (CAST(:status AS text) IS NULL OR b.status = CAST(:status AS text))
              AND (CAST(:errorCode AS text) IS NULL OR
            """ + ERROR_CODE + " = CAST(:errorCode AS text))";

    @Query(value = "SELECT b.barcode AS barcode, b.line_id AS lineId, b.cre_date AS creDate, b.status AS status, "
                   + ERROR_CODE + " AS errorCode " + SEARCH_FILTER,
           countQuery = "SELECT count(*) " + SEARCH_FILTER,
           nativeQuery = true)
    Page<BarcodeRowProjection> search(@Param("lineId") String lineId,
                                      @Param("barcodeQuery") String barcodeQuery,
                                      @Param("status") String status,
                                      @Param("errorCode") String errorCode,
                                      Pageable pageable);

    /**
     * Hatalı barkodların hata koduna göre adetleri, çoktan aza. lineId null ise tüm hatlar sayılır.
     * Hata türü sayısı birkaç tanedir; LIMIT yalnızca listeyi sınırsız bırakmamak için.
     */
    @Query(value = "SELECT " + ERROR_CODE + " AS code, count(*) AS count " + """
            FROM barcode_data b
            WHERE b.status = 'ERROR'
              AND (CAST(:lineId AS text) IS NULL OR b.line_id = CAST(:lineId AS text))
            GROUP BY 1
            ORDER BY 2 DESC, 1
            LIMIT 10
            """,
           nativeQuery = true)
    List<ErrorCountProjection> countErrors(@Param("lineId") String lineId);
}
