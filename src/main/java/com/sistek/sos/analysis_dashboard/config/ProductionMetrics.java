package com.sistek.sos.analysis_dashboard.config;

import com.sistek.sos.analysis_dashboard.dto.LineSummary;
import com.sistek.sos.analysis_dashboard.dto.PeriodFilter;
import com.sistek.sos.analysis_dashboard.dto.PlcSummary;
import com.sistek.sos.analysis_dashboard.services.LineService;
import com.sistek.sos.analysis_dashboard.services.PeriodService;
import com.sistek.sos.analysis_dashboard.services.PlcService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Prometheus metrikleri (/actuator/prometheus): BT'nin "toplayıcı sustu", "ERP'ye gönderim durdu", "hat duruyor" için
 * ekrana bakmadan alarm kurabilmesi. Değerler 15 sn'de bir tek seferde tazelenir; kazıma veritabanına sorgu atmaz.
 * Tazeleme hata verirse değerler kaldırılır: eski sayı güncelmiş gibi durmaz (panodaki bayat veri kuralının karşılığı).
 */
@Component
public class ProductionMetrics {

    private static final Logger log = LoggerFactory.getLogger(ProductionMetrics.class);
    /** Hat kartlarındaki varsayılan dönem: sayılar panonun "bu vardiya" görünümüyle aynıdır. */
    private static final PeriodFilter CURRENT_SHIFT = new PeriodFilter("shift", null, null);

    private final PlcService plcService;
    private final LineService lineService;
    private final PeriodService periodService;
    private final Clock clock;

    private final MultiGauge lineRunning;
    private final MultiGauge shiftBarcodes;
    private volatile double signalAgeSeconds = Double.NaN;
    private volatile double stuckBarcodes = Double.NaN;

    public ProductionMetrics(MeterRegistry registry, PlcService plcService, LineService lineService,
                             PeriodService periodService, Clock clock) {
        this.plcService = plcService;
        this.lineService = lineService;
        this.periodService = periodService;
        this.clock = clock;
        Gauge.builder("dashboard.collector.signal.age", this, m -> m.signalAgeSeconds)
                .description("PLC toplayıcıdan son sinyalden bu yana geçen süre (60 sn'yi aşarsa toplayıcı sustu)")
                .baseUnit("seconds")
                .register(registry);
        Gauge.builder("dashboard.barcodes.stuck", this, m -> m.stuckBarcodes)
                .description("Eşikten uzun süredir NEW, yani ERP'ye gönderilmeyi bekleyen barkod sayısı")
                .register(registry);
        lineRunning = MultiGauge.builder("dashboard.line.running")
                .description("Hat çalışıyor mu: 1 RUN, 0 STOP ya da PASSIVE")
                .register(registry);
        shiftBarcodes = MultiGauge.builder("dashboard.shift.barcodes")
                .description("Bu vardiyada okunan barkodlar, ERP aktarım durumuna göre (sent, error, pending)")
                .register(registry);
    }

    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.SECONDS)
    public void refresh() {
        try {
            PlcSummary plc = plcService.findFirst().orElse(null);
            List<LineSummary> lines = lineService.findAll(periodService.resolve(CURRENT_SHIFT));
            signalAgeSeconds = plc == null ? Double.NaN
                    : Duration.between(plc.lastSignalAt(), LocalDateTime.now(clock)).toSeconds();
            stuckBarcodes = plc == null ? Double.NaN : plcService.checkCollector(plc).stuckCount();
            lineRunning.register(lines.stream()
                    .map(line -> MultiGauge.Row.of(Tags.of("line", line.id()), "RUN".equals(line.status()) ? 1 : 0))
                    .toList(), true);
            shiftBarcodes.register(lines.stream()
                    .flatMap(line -> Stream.of(
                            MultiGauge.Row.of(Tags.of("line", line.id(), "status", "sent"), line.sent()),
                            MultiGauge.Row.of(Tags.of("line", line.id(), "status", "error"), line.error()),
                            MultiGauge.Row.of(Tags.of("line", line.id(), "status", "pending"), line.pending())))
                    .toList(), true);
        } catch (RuntimeException e) {
            log.warn("Metrikler tazelenemedi, değerler kaldırıldı: {}", e.getMessage());
            signalAgeSeconds = Double.NaN;
            stuckBarcodes = Double.NaN;
            lineRunning.register(List.of(), true);
            shiftBarcodes.register(List.of(), true);
        }
    }
}
