package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.dto.HourBar;
import com.sistek.sos.analysis_dashboard.dto.HourlyProduction;
import com.sistek.sos.analysis_dashboard.dto.Period;
import com.sistek.sos.analysis_dashboard.repositories.BarcodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Saatlik üretim: bir hattın seçilen dönemde saat saat okuduğu barkodlar. Toplama veritabanında yapılır (en fazla 24 kova);
 * tarayıcıya ham kayıt gitmez.
 */
@Service
@Transactional(readOnly = true)
public class HourlyProductionService {

    /** Bir saatlik kovanın sayıları; hourIndex dönem başından itibaren kaçıncı saat olduğudur. */
    record HourCount(int hourIndex, long total, long sent, long error, long pending) {
    }

    private final BarcodeRepository barcodeRepository;
    private final Clock clock;

    public HourlyProductionService(BarcodeRepository barcodeRepository, Clock clock) {
        this.barcodeRepository = barcodeRepository;
        this.clock = clock;
    }

    /** Dönem bir günü aşıyorsa ("tüm zamanlar") null: 24'ten fazla kova çizilmez. */
    public HourlyProduction find(String lineId, Period period) {
        if (!period.fitsInOneDay()) {
            return null;
        }
        List<HourCount> counts = barcodeRepository.countByHour(lineId, period.from(), period.to()).stream()
                .map(row -> new HourCount(row.getHourIndex(), row.getTotal(), row.getSent(), row.getError(), row.getPending()))
                .toList();
        return buckets(period.from(), period.to(), LocalDateTime.now(clock), counts);
    }

    /**
     * Dönemin her saati için bir çubuk; barkod olmayan saat 0'dır, henüz gelmemiş saat ayrıca işaretlenir.
     * Yükseklik en yüksek saate göre yüzdedir, eksen sıfırdan başlar.
     */
    static HourlyProduction buckets(LocalDateTime from, LocalDateTime to, LocalDateTime now, List<HourCount> counts) {
        Map<Integer, HourCount> byHour = counts.stream().collect(Collectors.toMap(HourCount::hourIndex, Function.identity()));
        long max = counts.stream().mapToLong(HourCount::total).max().orElse(0);
        int hourCount = (int) Math.ceil(Duration.between(from, to).toMinutes() / 60.0);

        List<HourBar> hours = new ArrayList<>();
        for (int i = 0; i < hourCount; i++) {
            LocalDateTime hourStart = from.plusHours(i);
            HourCount count = byHour.getOrDefault(i, new HourCount(i, 0, 0, 0, 0));
            hours.add(new HourBar(hourStart, count.total(), count.sent(), count.error(), count.pending(),
                    max == 0 ? 0 : count.total() * 100.0 / max, !hourStart.isBefore(now)));
        }
        return new HourlyProduction(hours, max);
    }
}
