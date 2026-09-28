package com.sistek.sos.analysis_dashboard.repositories;

import com.sistek.sos.analysis_dashboard.entities.BarcodeData;
import com.sistek.sos.analysis_dashboard.entities.BarcodeId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

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
     * Dinamik barkod filtresi: Gönderilen parametreler null ise filtre uygulanmaz. Dönem [from, to) UTC her zaman
     * uygulanır ("tüm zamanlar" da bir aralıktır).
     * CAST(... AS text): PostgreSQL'in null parametrelerde veri tipini belirleyebilmesi için gereklidir.
     * ILIKE: Barkod metninde büyük/küçük harf duyarsız arama yapar.
     */
    String SEARCH_FILTER = """
            FROM barcode_data b
            WHERE b.cre_date >= :from AND b.cre_date < :to
              AND (CAST(:lineId AS text) IS NULL OR b.line_id = CAST(:lineId AS text))
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
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to,
                                      Pageable pageable);

    /**
     * Dışa aktarım için aramayla aynı filtre, sayfasız ve akış halinde (WEB-05 §4.2): satırlar veritabanından parça parça
     * okunur, hepsi belleğe alınmaz. Açık bir işlem (transaction) içinde tüketilip kapatılmalıdır. Sıra, hat sayfasının
     * varsayılanıyla aynı (yeniden eskiye).
     */
    @QueryHints(@QueryHint(name = HibernateHints.HINT_FETCH_SIZE, value = "500"))
    @Query(value = "SELECT b.barcode AS barcode, b.line_id AS lineId, b.cre_date AS creDate, b.status AS status, "
                   + ERROR_CODE + " AS errorCode " + SEARCH_FILTER + " ORDER BY b.cre_date DESC, b.barcode DESC",
           nativeQuery = true)
    Stream<BarcodeRowProjection> streamForExport(@Param("lineId") String lineId,
                                                 @Param("barcodeQuery") String barcodeQuery,
                                                 @Param("status") String status,
                                                 @Param("errorCode") String errorCode,
                                                 @Param("from") LocalDateTime from,
                                                 @Param("to") LocalDateTime to);

    /**
     * Seçilen dönemdeki hatalı barkodların hata koduna göre adetleri, çoktan aza. lineId null ise tüm hatlar sayılır.
     * Hata türü sayısı birkaç tanedir; LIMIT yalnızca listeyi sınırsız bırakmamak için.
     */
    @Query(value = "SELECT " + ERROR_CODE + " AS code, count(*) AS count " + """
            FROM barcode_data b
            WHERE b.status = 'ERROR'
              AND b.cre_date >= :from AND b.cre_date < :to
              AND (CAST(:lineId AS text) IS NULL OR b.line_id = CAST(:lineId AS text))
            GROUP BY 1
            ORDER BY 2 DESC, 1
            LIMIT 10
            """,
           nativeQuery = true)
    List<ErrorCountProjection> countErrors(@Param("lineId") String lineId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    /**
     * Bir hattın [from, to) dönemindeki barkodları, dönem başından itibaren saat saat (0 = ilk saat). Kovalar dönem
     * başına göre sayılır; dönem yerel saat başında başladığı için kovalar da yerel saat başlarıdır. En fazla 24 satır
     * (dönem en fazla bir gün). Saati boş geçen kova dönmez.
     */
    @Query(value = """
            SELECT CAST(floor(extract(epoch FROM b.cre_date - CAST(:from AS timestamp)) / 3600) AS integer) AS hourIndex,
                   count(*) AS total,
                   count(*) FILTER (WHERE b.status = 'SENT') AS sent,
                   count(*) FILTER (WHERE b.status = 'ERROR') AS error,
                   count(*) FILTER (WHERE b.status = 'NEW') AS pending
            FROM barcode_data b
            WHERE b.line_id = :lineId AND b.cre_date >= :from AND b.cre_date < :to
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<HourlyCountProjection> countByHour(@Param("lineId") String lineId,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    /** Verilen durumda olup verilen UTC zamandan önce oluşmuş barkod sayısı (ör. takılı kalmış NEW'ler). */
    long countByStatusAndCreDateBefore(String status, LocalDateTime before);
}
