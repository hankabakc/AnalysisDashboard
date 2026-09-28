package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.config.ShiftSettings;
import com.sistek.sos.analysis_dashboard.dto.Period;
import com.sistek.sos.analysis_dashboard.dto.PeriodFilter;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Dönem seçimini (bu vardiya, bugün, belirli gün/vardiya) sorgular için UTC aralığına çevirir.
 * Vardiya saatleri fabrikanın yerel saatidir, yani sunucunun saat dilimi (bkz. DisplayTime); veritabanı saatleri UTC'dir.
 */
@Service
public class PeriodService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");

    private final ShiftSettings shifts;
    private final Clock clock;

    public PeriodService(ShiftSettings shifts, Clock clock) {
        this.shifts = shifts;
        this.clock = clock;
    }

    public Period resolve(PeriodFilter filter) {
        if (filter.day() != null) {
            return isShift(filter.shift()) ? shift(filter.day(), filter.shift(), "") : wholeDay(filter.day(), "");
        }
        LocalDateTime now = LocalDateTime.now(clock.withZone(ZoneId.systemDefault()));
        LocalDate today = productionDay(now);
        return switch (filter.range()) {
            case "all" -> Period.ALL;
            case "today" -> wholeDay(today, "Bugün: ");
            default -> shift(today, currentShift(today, now), "Bu vardiya: ");
        };
    }

    /** Vardiya seçim listesi: "1. vardiya (08:00–16:00)", ... */
    public List<String> shiftLabels() {
        return IntStream.rangeClosed(1, shifts.count()).mapToObj(this::shiftLabel).toList();
    }

    private Period shift(LocalDate day, int no, String prefix) {
        LocalDateTime dayStart = day.atTime(firstStart());
        return period(dayStart.plus(shifts.offset(no - 1)), dayStart.plus(shifts.offset(no)),
                prefix + DAY.format(day) + " · " + shiftLabel(no));
    }

    private Period wholeDay(LocalDate day, String prefix) {
        LocalDateTime dayStart = day.atTime(firstStart());
        String start = HOUR.format(firstStart());
        return period(dayStart, dayStart.plusDays(1),
                prefix + DAY.format(day) + " · tüm gün (" + start + "–" + start + ")");
    }

    private String shiftLabel(int no) {
        LocalTime start = shifts.starts().get(no - 1);
        LocalTime end = shifts.starts().get(no % shifts.count()); // son vardiya ilk vardiyanın başında biter
        return no + ". vardiya (" + HOUR.format(start) + "–" + HOUR.format(end) + ")";
    }

    /** Üretim günü ilk vardiyanın başında başlar: ondan önceki gece saatleri önceki güne aittir. */
    private LocalDate productionDay(LocalDateTime now) {
        return now.toLocalTime().isBefore(firstStart()) ? now.toLocalDate().minusDays(1) : now.toLocalDate();
    }

    private int currentShift(LocalDate day, LocalDateTime now) {
        Duration sinceDayStart = Duration.between(day.atTime(firstStart()), now);
        int no = 1;
        while (no < shifts.count() && shifts.offset(no).compareTo(sinceDayStart) <= 0) {
            no++;
        }
        return no;
    }

    private boolean isShift(Integer no) {
        return no != null && no >= 1 && no <= shifts.count();
    }

    private LocalTime firstStart() {
        return shifts.starts().get(0);
    }

    private static Period period(LocalDateTime localFrom, LocalDateTime localTo, String label) {
        return new Period(toUtc(localFrom), toUtc(localTo), label);
    }

    private static LocalDateTime toUtc(LocalDateTime local) {
        return local.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
