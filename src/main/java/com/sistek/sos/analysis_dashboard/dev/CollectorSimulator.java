package com.sistek.sos.analysis_dashboard.dev;

import com.sistek.sos.analysis_dashboard.dto.ErpError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Geliştirme veri simülatörü: PLC toplayıcıyı taklit eder (APP-01 §2.6, önce sahte uygulama).
 * Gerçek PLC ve ERP olmadan panoyu canlı veriyle geliştirmek ve göstermek içindir.
 *
 * Uygulamanın geri kalanı PLC tablolarına yazmaz; bu sınıf toplayıcının yerine geçtiği için yazar. Bu yüzden üç kilit var:
 * yalnızca dev profilinde, dashboard.simulator.enabled=true iken ve yalnızca yerel veritabanına (localhost) karşı çalışır.
 * Yalnızca kendi "SIM" barkodlarını işler. Temizlik: DELETE FROM barcode_data WHERE barcode LIKE 'SIM%'
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "dashboard.simulator.enabled", havingValue = "true")
@EnableScheduling
public class CollectorSimulator {

    private static final Logger log = LoggerFactory.getLogger(CollectorSimulator.class);
    private static final DateTimeFormatter BARCODE_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    // Tik (5 sn) başına olasılıklar: çalışan hat dakikada ~2 barkod okur, ~4 dk'da bir durur; duran hat ~25 sn'de kalkar
    private static final double BARCODE_CHANCE = 0.2;
    private static final double STOP_CHANCE = 0.02;
    private static final double RESTART_CHANCE = 0.2;
    /** Toplayıcı barkodu okuduktan birkaç saniye sonra ERP'ye gönderir (gerçek veride ortalama 3 sn). */
    private static final Duration ERP_DELAY = Duration.ofSeconds(3);

    private final JdbcTemplate jdbc;
    private final Random random;

    @Autowired
    public CollectorSimulator(JdbcTemplate jdbc, @Value("${spring.datasource.url}") String datasourceUrl) {
        this(jdbc, datasourceUrl, new Random());
    }

    /** Testler tekrarlanabilir sonuç için tohumlu Random verir. */
    CollectorSimulator(JdbcTemplate jdbc, String datasourceUrl, Random random) {
        if (!datasourceUrl.contains("//localhost") && !datasourceUrl.contains("//127.0.0.1")) {
            throw new IllegalStateException("Simülatör yalnızca yerel veritabanına yazar; sahte veri gerçek toplayıcının "
                    + "tablolarına karışmasın diye açılış durduruldu. Adres: " + datasourceUrl
                    + " (kapatmak için dashboard.simulator.enabled=false)");
        }
        this.jdbc = jdbc;
        this.random = random;
        log.warn("Geliştirme veri simülatörü açık: PLC tablolarına sahte veri yazılıyor ({})", datasourceUrl);
    }

    /** Gerçek toplayıcının kalp atışı sıklığı. Veritabanı saatleri UTC'dir. */
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS)
    public void run() {
        tick(LocalDateTime.now(ZoneOffset.UTC));
    }

    void tick(LocalDateTime now) {
        heartbeat(now);
        for (Map<String, Object> line : jdbc.queryForList("SELECT line_id, status FROM line_info")) {
            String lineId = (String) line.get("line_id");
            String status = changeLineStatus(lineId, (String) line.get("status"), now);
            if ("RUN".equals(status) && random.nextDouble() < BARCODE_CHANCE) {
                jdbc.update("INSERT INTO barcode_data (barcode, line_id, cre_date, status) VALUES (?, ?, ?, 'NEW')",
                        "SIM" + BARCODE_TIME.format(now) + lineId, lineId, now);
            }
        }
        sendToErp(now);
    }

    private void heartbeat(LocalDateTime now) {
        jdbc.update("UPDATE plc_info SET last_status_date = ?, status = 'ACTIVE'", now);
        jdbc.update("""
                INSERT INTO plc_log (plc_id, seq_no, proc_date, status)
                SELECT plc_id, (SELECT coalesce(max(seq_no), 0) + 1 FROM plc_log), ?, 'ACTIVE' FROM plc_info
                """, now);
    }

    /** Çalışan hat ara sıra durur, duran hat kısa sürede kalkar; PASSIVE hatlara dokunulmaz. */
    private String changeLineStatus(String lineId, String status, LocalDateTime now) {
        String next = switch (status) {
            case "RUN" -> random.nextDouble() < STOP_CHANCE ? "STOP" : "RUN";
            case "STOP" -> random.nextDouble() < RESTART_CHANCE ? "RUN" : "STOP";
            default -> status;
        };
        if (!next.equals(status)) {
            jdbc.update("UPDATE line_info SET status = ?, last_status_date = ? WHERE line_id = ?", next, now, lineId);
            jdbc.update("""
                    INSERT INTO line_log (line_id, seq_no, proc_date, status)
                    VALUES (?, (SELECT coalesce(max(seq_no), 0) + 1 FROM line_log), ?, ?)
                    """, lineId, now, next);
        }
        return next;
    }

    /** ERP_DELAY'den eski SIM barkodlarını "ERP'ye gönderir": çoğu SENT, kalanı gerçekteki hata biçimleriyle ERROR. */
    private void sendToErp(LocalDateTime now) {
        List<Map<String, Object>> waiting = jdbc.queryForList(
                "SELECT barcode, line_id FROM barcode_data WHERE status = 'NEW' AND barcode LIKE 'SIM%' AND cre_date <= ?",
                now.minus(ERP_DELAY));
        for (Map<String, Object> row : waiting) {
            String barcode = (String) row.get("barcode");
            ErpError error = erpError();
            jdbc.update("UPDATE barcode_data SET status = ?, api_result = ?, last_status_date = ? WHERE barcode = ? AND line_id = ?",
                    error == null ? "SENT" : "ERROR",
                    error == null ? "{\"Success\":\"TRUE\"}" : apiResult(error, barcode),
                    now, barcode, row.get("line_id"));
        }
    }

    /** ERP'nin cevabı: null başarılı aktarım demektir. Oranlar: %85 başarılı, %8 iş emri yok, %4 format, %3 bağlantı. */
    private ErpError erpError() {
        double r = random.nextDouble();
        if (r < 0.85) {
            return null;
        }
        if (r < 0.93) {
            return ErpError.ORDER_NOT_FOUND;
        }
        return r < 0.97 ? ErpError.BAD_FORMAT : ErpError.CONNECTION;
    }

    /** Gerçek toplayıcının api_result biçimleri; bağlantı hatasındaki sunucu adresi yer tutucudur. */
    static String apiResult(ErpError error, String barcode) {
        return switch (error) {
            case ORDER_NOT_FOUND -> "{\"Success\":\"FALSE\",\"ErrorText\":\"ORA-20100: Error.bu barkodun bagli oldugu is emri bulunamadi"
                    + barcode + "\"}";
            case BAD_FORMAT -> "{\"Success\":\"FALSE\",\"ErrorText\":\"ORA-20124: TrautSync.FND_ITEM_FORMAT: The value \\\"BARCODE_ID="
                    + barcode + "\\\" has incorrect format for [SYSTEM_KEY] in Traut Sync.\"}";
            case CONNECTION -> "Failed to send barcode request: I/O error on POST request for "
                    + "\"https://erp.example.invalid/ReceiveBarcode\": Connection refused";
        };
    }
}
