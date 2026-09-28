package com.sistek.sos.analysis_dashboard.dev;

import com.sistek.sos.analysis_dashboard.dto.ErpError;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Geliştirme veri simülatörü: PLC toplayıcıyı taklit eder (APP-01 §2.6, önce sahte uygulama).
 * Gerçek PLC ve ERP olmadan panoyu canlı veriyle geliştirmek ve göstermek içindir. Açılışta son 30 günün geçmişini de
 * bir kez üretir; geçmiş gün ve vardiyaya bakan ekranlar (duruş analizi, saatlik üretim, hata dağılımı) boş kalmaz.
 *
 * Uygulamanın geri kalanı PLC tablolarına yazmaz; bu sınıf toplayıcının yerine geçtiği için yazar. Bu yüzden üç kilit var:
 * yalnızca dev profilinde, dashboard.simulator.enabled=true iken ve yalnızca yerel veritabanına (localhost) karşı çalışır.
 * Yalnızca kendi "SIM" barkodlarını işler. Temizlik komutları application-dev.properties'tedir.
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

    /** Açılışta üretilen geçmişin uzunluğu: geçmiş gün ve vardiyalara bakan ekranlar boş kalmasın. */
    private static final Duration HISTORY = Duration.ofDays(30);
    // Geçmiş dakika adımlıdır: çalışan hat dakikada 1/90 olasılıkla durur, duran hat 1/8 ile kalkar (ortalama 8 dk duruş)
    private static final double HISTORY_STOP_CHANCE = 1.0 / 90;
    private static final double HISTORY_RESTART_CHANCE = 1.0 / 8;
    /** Geçmişteki kalp atışı aralığı: sessizlik eşiğinin (60 sn) altında; gerçek 5 sn'lik sıklık 6 kat satır üretirdi. */
    private static final Duration HISTORY_BEAT = Duration.ofSeconds(30);

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
            ErpAnswer answer = erpAnswer(barcode);
            jdbc.update("UPDATE barcode_data SET status = ?, api_result = ?, last_status_date = ? WHERE barcode = ? AND line_id = ?",
                    answer.status(), answer.apiResult(), now, barcode, row.get("line_id"));
        }
    }

    /**
     * Açılışta geçmişi doldurur. Zamanlanmış tik bu metot bitmeden başlamaz, bu yüzden seq_no'lar yarışmaz.
     * Tek işlemdir: yarıda kalırsa hiçbir şey yazılmaz (ve 70 bin satır tek tek commit edilmez).
     */
    @PostConstruct
    void backfillHistory() {
        long started = System.currentTimeMillis();
        Long inserted = new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()))
                .execute(status -> backfill(LocalDateTime.now(ZoneOffset.UTC), HISTORY));
        if (inserted != null && inserted > 0) {
            log.info("Geliştirme geçmişi üretildi: son {} gün, {} barkod ({} ms)", HISTORY.toDays(), inserted,
                    System.currentTimeMillis() - started);
        }
    }

    /**
     * [now − span, en eski SIM barkodu) aralığını toplayıcı o sırada çalışıyormuş gibi doldurur: hat durum değişimleri,
     * barkodlar (ERP'ye işlenmiş; NEW bırakılmaz) ve kalp atışları. Eklenen barkod sayısını döner. Aralık bir saatten
     * kısaysa geçmiş zaten üretilmiştir ve hiçbir şey yazılmaz; yani tekrar açılışta çift veri oluşmaz.
     * Dakika adımlıdır; PASSIVE hatlar geçmiş almaz, diğerleri RUN başlar.
     */
    long backfill(LocalDateTime now, Duration span) {
        LocalDateTime from = now.minus(span).truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime earliest = jdbc.queryForObject(
                "SELECT min(cre_date) FROM barcode_data WHERE barcode LIKE 'SIM%'", LocalDateTime.class);
        LocalDateTime to = earliest != null && earliest.isBefore(now) ? earliest : now;
        if (Duration.between(from, to).compareTo(Duration.ofHours(1)) < 0) {
            return 0;
        }

        List<Object[]> statusChanges = new ArrayList<>();
        List<Object[]> barcodes = new ArrayList<>();
        long seqNo = jdbc.queryForObject("SELECT coalesce(max(seq_no), 0) FROM line_log", Long.class);
        for (String lineId : jdbc.queryForList(
                "SELECT line_id FROM line_info WHERE status <> 'PASSIVE' ORDER BY line_id", String.class)) {
            String status = "RUN";
            statusChanges.add(new Object[]{lineId, ++seqNo, from, status});
            for (LocalDateTime minute = from; minute.isBefore(to); minute = minute.plusMinutes(1)) {
                String next = switch (status) {
                    case "RUN" -> random.nextDouble() < HISTORY_STOP_CHANCE ? "STOP" : "RUN";
                    default -> random.nextDouble() < HISTORY_RESTART_CHANCE ? "RUN" : "STOP";
                };
                if (!next.equals(status)) {
                    status = next;
                    statusChanges.add(new Object[]{lineId, ++seqNo, minute, status});
                }
                LocalDateTime readAt = minute.plusSeconds(random.nextInt(60));
                if ("RUN".equals(status) && readAt.isBefore(to) && random.nextDouble() < barcodeChance(readAt)) {
                    String barcode = "SIM" + BARCODE_TIME.format(readAt) + lineId;
                    ErpAnswer answer = erpAnswer(barcode);
                    barcodes.add(new Object[]{barcode, lineId, readAt, answer.status(), answer.apiResult(), readAt.plus(ERP_DELAY)});
                }
            }
        }

        jdbc.batchUpdate("INSERT INTO line_log (line_id, seq_no, proc_date, status) VALUES (?, ?, ?, ?)", statusChanges);
        jdbc.batchUpdate("""
                INSERT INTO barcode_data (barcode, line_id, cre_date, status, api_result, last_status_date)
                VALUES (?, ?, ?, ?, ?, ?)
                """, barcodes);
        long beatSeconds = HISTORY_BEAT.toSeconds();
        jdbc.update("""
                INSERT INTO plc_log (plc_id, seq_no, proc_date, status)
                SELECT p.plc_id, m.last_seq + s.n + 1, CAST(? AS timestamp) + make_interval(secs => s.n * CAST(? AS integer)), 'ACTIVE'
                FROM plc_info p, (SELECT coalesce(max(seq_no), 0) AS last_seq FROM plc_log) m, generate_series(0, ?) s(n)
                """, from, beatSeconds, (Duration.between(from, to).toSeconds() - 1) / beatSeconds);
        return barcodes.size();
    }

    /** Dakikada barkod olasılığı, vardiyaya göre (yerel saat): gündüz yoğun, gece seyrek → hat başına ~600 barkod/gün. */
    private static double barcodeChance(LocalDateTime utc) {
        int hour = utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.systemDefault()).getHour();
        if (hour >= 8 && hour < 16) {
            return 0.5;
        }
        return hour >= 16 ? 0.45 : 0.3;
    }

    /** ERP'nin bir barkoda cevabı: durum ve toplayıcının yazdığı api_result. */
    private record ErpAnswer(String status, String apiResult) {
    }

    private ErpAnswer erpAnswer(String barcode) {
        ErpError error = erpError();
        return error == null
                ? new ErpAnswer("SENT", "{\"Success\":\"TRUE\"}")
                : new ErpAnswer("ERROR", apiResult(error, barcode));
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
