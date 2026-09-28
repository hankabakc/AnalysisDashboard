package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.dto.Downtime;
import com.sistek.sos.analysis_dashboard.dto.Period;
import com.sistek.sos.analysis_dashboard.dto.StatusSegment;
import com.sistek.sos.analysis_dashboard.repositories.LineLogRepository;
import com.sistek.sos.analysis_dashboard.repositories.LineStatusProjection;
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
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Hat duruş analizi: line_log'daki durum değişimlerinden seçilen dönemin zaman çizelgesi, çalışma/duruş süreleri
 * ve kullanılabilirlik. Zamanlar UTC'dir.
 */
@Service
@Transactional(readOnly = true)
public class DowntimeService {

    /** İlk kayıttan önceki, durumu bilinmeyen süre. */
    static final String UNKNOWN = "UNKNOWN";

    /** Analiz vardiya ya da gün için yapılır; "tüm zamanlar" bütün log tablosunu çekmek, yani sınırsız sorgu olurdu. */
    private static final Duration MAX_PERIOD = Duration.ofDays(1);
    private static final int LONGEST_STOPS = 5;

    /** line_log'daki bir durum değişimi. */
    record StatusChange(LocalDateTime at, String status) {
    }

    private final LineLogRepository lineLogRepository;
    private final Clock clock;

    public DowntimeService(LineLogRepository lineLogRepository, Clock clock) {
        this.lineLogRepository = lineLogRepository;
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
        Set<String> lineIds = new TreeSet<>(initialStatus.keySet());
        lineIds.addAll(changes.keySet());
        Map<String, Downtime> result = new HashMap<>();
        for (String id : lineIds) {
            result.put(id, analyze(initialStatus.getOrDefault(id, UNKNOWN), changes.getOrDefault(id, List.of()),
                    period.from(), period.to(), now));
        }
        return result;
    }

    /**
     * Dönem [from, to) için çizelge; süren dönem "now"da kesilir. changes zaman sırasıyla gelir.
     * Toplayıcı yeniden başlayınca aynı durumu tekrar yazar: ardışık aynı durumlar tek parçadır.
     */
    static Downtime analyze(String initialStatus, List<StatusChange> changes, LocalDateTime from, LocalDateTime to,
                            LocalDateTime now) {
        LocalDateTime end = now.isBefore(to) ? now : to;
        double periodMillis = Duration.between(from, to).toMillis();
        List<StatusSegment> segments = new ArrayList<>();

        String status = initialStatus;
        LocalDateTime segmentStart = from;
        for (StatusChange change : changes) {
            if (!change.at().isBefore(end)) {
                break;
            }
            if (!change.status().equals(status)) {
                addSegment(segments, status, segmentStart, change.at(), periodMillis);
                status = change.status();
                segmentStart = change.at();
            }
        }
        addSegment(segments, status, segmentStart, end, periodMillis);

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

    /** Sıfır ya da negatif uzunluktaki parça (ör. dönemin ilk anındaki kayıt, gelecekteki dönem) eklenmez. */
    private static void addSegment(List<StatusSegment> segments, String status, LocalDateTime start, LocalDateTime end,
                                   double periodMillis) {
        if (start.isBefore(end)) {
            segments.add(new StatusSegment(status, start, end, Duration.between(start, end).toMillis() * 100.0 / periodMillis));
        }
    }

    private static Duration total(List<StatusSegment> segments, String status) {
        return segments.stream()
                .filter(segment -> status.equals(segment.status()))
                .map(StatusSegment::duration)
                .reduce(Duration.ZERO, Duration::plus);
    }
}
