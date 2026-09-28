package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.config.SourceSettings;
import com.sistek.sos.analysis_dashboard.dto.Downtime;
import com.sistek.sos.analysis_dashboard.dto.Period;
import com.sistek.sos.analysis_dashboard.dto.StatusSegment;
import com.sistek.sos.analysis_dashboard.repositories.LineLogRepository;
import com.sistek.sos.analysis_dashboard.repositories.LineStatusProjection;
import com.sistek.sos.analysis_dashboard.repositories.PlcLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Hat duruş analizi: line_log'daki durum değişimlerinden seçilen dönemin zaman çizelgesi, çalışma/duruş süreleri
 * ve kullanılabilirlik. Toplayıcının sustuğu aralıklar (plc_log kalp atışında boşluk) "Veri yok" sayılır: son bilinen
 * durum, toplayıcı sustuğunda sürmüş kabul edilmez. Zamanlar UTC'dir.
 */
@Service
@Transactional(readOnly = true)
public class DowntimeService {

    /** Durumu bilinmeyen süre: hattın ilk kaydından önce ya da toplayıcı sustuğunda. */
    static final String UNKNOWN = "UNKNOWN";

    /** Analiz vardiya ya da gün için yapılır; "tüm zamanlar" bütün log tablosunu çekmek, yani sınırsız sorgu olurdu. */
    private static final Duration MAX_PERIOD = Duration.ofDays(1);
    private static final int LONGEST_STOPS = 5;

    /** line_log'daki bir durum değişimi. */
    record StatusChange(LocalDateTime at, String status) {
    }

    /** Toplayıcının sustuğu aralık: [from, to). */
    record Silence(LocalDateTime from, LocalDateTime to) {
        boolean contains(LocalDateTime time) {
            return !time.isBefore(from) && time.isBefore(to);
        }
    }

    private final LineLogRepository lineLogRepository;
    private final PlcLogRepository plcLogRepository;
    private final SourceSettings sourceSettings;
    private final Clock clock;

    public DowntimeService(LineLogRepository lineLogRepository, PlcLogRepository plcLogRepository,
                           SourceSettings sourceSettings, Clock clock) {
        this.lineLogRepository = lineLogRepository;
        this.plcLogRepository = plcLogRepository;
        this.sourceSettings = sourceSettings;
        this.clock = clock;
    }

    public boolean supports(Period period) {
        return Duration.between(period.from(), period.to()).compareTo(MAX_PERIOD) <= 0;
    }

    /**
     * Hat kimliği → analiz. lineId null ise tüm hatlar. Dönem desteklenmiyorsa boş döner;
     * hiç durum kaydı olmayan hat haritada yer almaz.
     */
    public Map<String, Downtime> analyze(Period period, String lineId) {
        if (!supports(period)) {
            return Map.of();
        }
        Map<String, String> initialStatus = lineLogRepository.findLastStatusBefore(period.from(), lineId).stream()
                .collect(Collectors.toMap(LineStatusProjection::getLineId, LineStatusProjection::getStatus));
        Map<String, List<StatusChange>> changes = lineLogRepository.findStatusChanges(period.from(), period.to(), lineId).stream()
                .collect(Collectors.groupingBy(LineStatusProjection::getLineId,
                        Collectors.mapping(log -> new StatusChange(log.getProcDate(), log.getStatus()), Collectors.toList())));

        LocalDateTime now = LocalDateTime.now(clock);
        List<Silence> silences = collectorSilences(period.from(), now.isBefore(period.to()) ? now : period.to());
        Set<String> lineIds = new TreeSet<>(initialStatus.keySet());
        lineIds.addAll(changes.keySet());
        Map<String, Downtime> result = new HashMap<>();
        for (String id : lineIds) {
            result.put(id, analyze(initialStatus.getOrDefault(id, UNKNOWN), changes.getOrDefault(id, List.of()), silences,
                    period.from(), period.to(), now));
        }
        return result;
    }

    /** [from, end) içinde toplayıcının sustuğu aralıklar (tek PLC toplayıcısı; tüm hatlar için aynı). */
    private List<Silence> collectorSilences(LocalDateTime from, LocalDateTime end) {
        if (!end.isAfter(from)) {
            return List.of();
        }
        Duration silentAfter = sourceSettings.silentAfter();
        return plcLogRepository.findCollectorSilences(from.minus(silentAfter), end, end.plus(silentAfter), silentAfter.toSeconds())
                .stream()
                .map(silence -> new Silence(silence.getSilentFrom(), silence.getSilentTo()))
                .toList();
    }

    /**
     * Dönem [from, to) için çizelge; süren dönem "now"da kesilir. changes zaman sırasıyla gelir.
     * Çizelgenin kesim noktaları durum değişimleri ve sessizlik sınırlarıdır. Her aralığın durumu, toplayıcı sustuysa
     * "Veri yok", değilse o anki hat durumudur. Toplayıcı yeniden başlayınca aynı durumu tekrar yazar: ardışık aynı
     * durumlar tek parçadır.
     */
    static Downtime analyze(String initialStatus, List<StatusChange> changes, List<Silence> silences,
                            LocalDateTime from, LocalDateTime to, LocalDateTime now) {
        LocalDateTime end = now.isBefore(to) ? now : to;
        double periodMillis = Duration.between(from, to).toMillis();
        List<StatusSegment> segments = new ArrayList<>();

        if (end.isAfter(from)) {
            // Hattın durumu: dönem başındaki durum ve değişimler (aynı anda iki kayıt varsa sonuncusu geçerli)
            TreeMap<LocalDateTime, String> statusSince = new TreeMap<>();
            statusSince.put(from, initialStatus);
            changes.stream()
                    .filter(change -> change.at().isBefore(end))
                    .forEach(change -> statusSince.put(change.at(), change.status()));

            TreeSet<LocalDateTime> cuts = new TreeSet<>(statusSince.keySet());
            for (Silence silence : silences) {
                cuts.add(clamp(silence.from(), from, end));
                cuts.add(clamp(silence.to(), from, end));
            }
            cuts.add(end);

            String openStatus = null;
            LocalDateTime openStart = null;
            LocalDateTime previous = null;
            for (LocalDateTime cut : cuts) {
                if (previous != null) {
                    LocalDateTime start = previous;
                    String status = silences.stream().anyMatch(silence -> silence.contains(start))
                            ? UNKNOWN : statusSince.floorEntry(start).getValue();
                    if (!status.equals(openStatus)) {
                        addSegment(segments, openStatus, openStart, start, periodMillis);
                        openStatus = status;
                        openStart = start;
                    }
                }
                previous = cut;
            }
            addSegment(segments, openStatus, openStart, end, periodMillis);
        }

        Duration run = total(segments, "RUN");
        Duration stop = total(segments, "STOP");
        List<StatusSegment> stops = segments.stream()
                .filter(segment -> "STOP".equals(segment.status()))
                .sorted(Comparator.comparing(StatusSegment::duration).reversed())
                .toList();
        long measured = run.plus(stop).toMillis();
        return new Downtime(
                segments,
                run,
                stop,
                stops.size(),
                stops.isEmpty() ? Duration.ZERO : stops.get(0).duration(),
                measured == 0 ? null : (int) Math.round(run.toMillis() * 100.0 / measured),
                stops.stream().limit(LONGEST_STOPS).toList());
    }

    /** Açık parça yoksa (döngünün ilk adımı) ya da parça boşsa eklenmez. */
    private static void addSegment(List<StatusSegment> segments, String status, LocalDateTime start, LocalDateTime end,
                                   double periodMillis) {
        if (status != null && start.isBefore(end)) {
            segments.add(new StatusSegment(status, start, end, Duration.between(start, end).toMillis() * 100.0 / periodMillis));
        }
    }

    private static LocalDateTime clamp(LocalDateTime time, LocalDateTime min, LocalDateTime max) {
        return time.isBefore(min) ? min : time.isAfter(max) ? max : time;
    }

    private static Duration total(List<StatusSegment> segments, String status) {
        return segments.stream()
                .filter(segment -> status.equals(segment.status()))
                .map(StatusSegment::duration)
                .reduce(Duration.ZERO, Duration::plus);
    }
}
