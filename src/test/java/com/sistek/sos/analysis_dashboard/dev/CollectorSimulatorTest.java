package com.sistek.sos.analysis_dashboard.dev;

import com.sistek.sos.analysis_dashboard.TestcontainersConfig;
import com.sistek.sos.analysis_dashboard.dto.ErpError;
import com.sistek.sos.analysis_dashboard.repositories.BarcodeRepository;
import com.sistek.sos.analysis_dashboard.repositories.PlcLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sahte toplayıcı: gerçek PostgreSQL'e yazar, her test sonunda yazdıkları geri alınır (@Transactional).
 * Rastgelelik tohumlu Random ile tekrarlanabilir; yine de kontroller sonuca değil kurallara bakar.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
class CollectorSimulatorTest {

    private static final String LOCAL_DB = "jdbc:postgresql://localhost:5435/postgres";
    private static final LocalDateTime START = LocalDateTime.parse("2025-07-31T09:43:00");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PlcLogRepository plcLogRepository;

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    @Test
    @Transactional
    @DisplayName("30 tik (5 sn arayla): kalp atışı, plc_log, SIM barkodları, 3 sn'den eskiler ERP'ye işlenmiş, hatalar tanınıyor, hat durumu log ile tutarlı")
    void ticksBehaveLikeTheCollector() {
        CollectorSimulator simulator = new CollectorSimulator(jdbc, LOCAL_DB, new Random(7));
        LocalDateTime last = START;
        for (int i = 0; i < 30; i++) {
            last = START.plusSeconds(5L * i);
            simulator.tick(last);
        }

        assertEquals(last, jdbc.queryForObject("SELECT last_status_date FROM plc_info", LocalDateTime.class));
        assertEquals(30, count("SELECT count(*) FROM plc_log WHERE proc_date >= ?", START));
        assertTrue(count("SELECT count(*) FROM barcode_data WHERE barcode LIKE 'SIM%'") > 0, "hiç SIM barkodu yok");
        assertEquals(0, count("SELECT count(*) FROM barcode_data WHERE barcode LIKE 'SIM%' AND status = 'NEW' AND cre_date <= ?",
                last.minusSeconds(3)), "ERP'ye işlenmemiş eski SIM barkodu kaldı");
        assertEquals(0, count("SELECT count(*) FROM barcode_data b WHERE b.barcode LIKE 'SIM%' AND b.status = 'ERROR' AND "
                + BarcodeRepository.ERROR_CODE + " IS NULL"), "tanınmayan hata metni");
        assertEquals(0, count("""
                SELECT count(*) FROM line_info i
                WHERE i.last_status_date >= ?
                  AND i.status <> (SELECT l.status FROM line_log l WHERE l.line_id = i.line_id ORDER BY l.seq_no DESC LIMIT 1)
                """, START), "line_info son line_log ile çelişiyor");
    }

    @Test
    @Transactional
    @DisplayName("Simülatör yalnızca kendi SIM barkodlarını işler: elle eklenen NEW takılı kalır")
    void leavesForeignNewBarcodesAlone() {
        jdbc.update("INSERT INTO barcode_data (barcode, line_id, cre_date, status) VALUES ('ELLE-1', '1', ?, 'NEW')", START);
        CollectorSimulator simulator = new CollectorSimulator(jdbc, LOCAL_DB, new Random(7));
        simulator.tick(START.plusSeconds(10));
        assertEquals("NEW", jdbc.queryForObject("SELECT status FROM barcode_data WHERE barcode = 'ELLE-1'", String.class));
    }

    @Test
    @Transactional
    @DisplayName("Geçmiş (2 gün): her aktif hatta barkod, PASSIVE hatta yok, NEW yok, barkod yalnızca RUN iken, toplayıcı hiç susmamış")
    void backfillLooksLikeTheCollectorsHistory() {
        jdbc.update("UPDATE line_info SET status = 'PASSIVE' WHERE line_id = '3'");
        long foreignBarcodes = count("SELECT count(*) FROM barcode_data");
        long lineLogBefore = count("SELECT max(seq_no) FROM line_log");
        LocalDateTime from = START.minusDays(2);

        long inserted = new CollectorSimulator(jdbc, LOCAL_DB, new Random(7)).backfill(START, Duration.ofDays(2));

        assertEquals(inserted, count("SELECT count(*) FROM barcode_data WHERE barcode LIKE 'SIM%'"));
        assertEquals(foreignBarcodes, count("SELECT count(*) FROM barcode_data WHERE barcode NOT LIKE 'SIM%'"));
        assertEquals(0, count("""
                SELECT count(*) FROM line_info i
                WHERE i.status <> 'PASSIVE' AND NOT EXISTS (SELECT 1 FROM barcode_data b WHERE b.line_id = i.line_id AND b.barcode LIKE 'SIM%')
                """), "barkodsuz aktif hat");
        assertEquals(0, count("SELECT count(*) FROM barcode_data WHERE barcode LIKE 'SIM%' AND line_id = '3'"), "PASSIVE hatta barkod");
        assertEquals(0, count("SELECT count(*) FROM barcode_data WHERE barcode LIKE 'SIM%' AND (status = 'NEW' OR cre_date < ? OR cre_date >= ?)",
                from, START), "NEW ya da pencere dışı barkod");
        assertEquals(0, count("SELECT count(*) FROM barcode_data b WHERE b.barcode LIKE 'SIM%' AND b.status = 'ERROR' AND "
                + BarcodeRepository.ERROR_CODE + " IS NULL"), "tanınmayan hata metni");
        assertEquals(0, count("""
                SELECT count(*) FROM barcode_data b
                WHERE b.barcode LIKE 'SIM%'
                  AND (SELECT l.status FROM line_log l WHERE l.line_id = b.line_id AND l.seq_no > ? AND l.proc_date <= b.cre_date
                       ORDER BY l.proc_date DESC, l.seq_no DESC LIMIT 1) IS DISTINCT FROM 'RUN'
                """, lineLogBefore), "hat RUN değilken okunan barkod");
        assertTrue(count("SELECT count(*) FROM line_log WHERE seq_no > ? AND status = 'STOP'", lineLogBefore) > 0, "hiç duruş yok");
        assertEquals(List.of(), plcLogRepository.findCollectorSilences(from, START, START, 60), "toplayıcı sessizliği");
    }

    @Test
    @Transactional
    @DisplayName("Geçmiş bir kez üretilir: ikinci açılış en eski SIM barkoduna kadar dolu pencereyi yeniden doldurmaz")
    void backfillRunsOnce() {
        CollectorSimulator simulator = new CollectorSimulator(jdbc, LOCAL_DB, new Random(7));
        simulator.backfill(START, Duration.ofDays(1));
        long barcodes = count("SELECT count(*) FROM barcode_data");
        long beats = count("SELECT count(*) FROM plc_log");

        assertEquals(0, simulator.backfill(START.plusMinutes(10), Duration.ofDays(1)));
        assertEquals(barcodes, count("SELECT count(*) FROM barcode_data"));
        assertEquals(beats, count("SELECT count(*) FROM plc_log"));
    }

    @Test
    @DisplayName("Üç hata metni panonun ERROR_CODE ifadesiyle ORA-20100 / ORA-20124 / CONNECTION olarak sınıflanır")
    void errorTextsMatchTheErrorCodeExpression() {
        Map<ErpError, String> expected = Map.of(
                ErpError.ORDER_NOT_FOUND, "ORA-20100",
                ErpError.BAD_FORMAT, "ORA-20124",
                ErpError.CONNECTION, "CONNECTION");
        expected.forEach((error, code) -> assertEquals(code, jdbc.queryForObject(
                "SELECT " + BarcodeRepository.ERROR_CODE + " FROM (SELECT CAST(? AS text) AS api_result) b",
                String.class, CollectorSimulator.apiResult(error, "SIM123"))));
    }

    @Test
    @DisplayName("Kilitler: yerel olmayan veritabanıyla kurulamaz; test bağlamında (dev profili yok) bean hiç oluşmaz")
    void onlyRunsAgainstLocalDevDatabase() {
        assertThrows(IllegalStateException.class,
                () -> new CollectorSimulator(jdbc, "jdbc:postgresql://10.0.0.5:5432/fabrika", new Random()));
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(CollectorSimulator.class));
    }
}
