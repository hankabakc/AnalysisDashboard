package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.dto.Downtime;
import com.sistek.sos.analysis_dashboard.dto.StatusSegment;
import com.sistek.sos.analysis_dashboard.services.DowntimeService.StatusChange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Duruş analizi hesabı (veritabanı gerekmez). Dönem 8 saatlik bir vardiya: [05:00, 13:00) UTC.
 */
class DowntimeServiceTest {

    private static final LocalDateTime FROM = LocalDateTime.parse("2025-07-31T05:00");
    private static final LocalDateTime TO = FROM.plusHours(8);

    private static StatusChange at(int minute, String status) {
        return new StatusChange(FROM.plusMinutes(minute), status);
    }

    private static Downtime analyze(String initialStatus, List<StatusChange> changes, int nowMinute) {
        return DowntimeService.analyze(initialStatus, changes, FROM, TO, FROM.plusMinutes(nowMinute));
    }

    @Test
    @DisplayName("Kayıt yok: geçen süre tek 'Veri yok' parçası; kullanılabilirlik hesaplanmaz")
    void noData() {
        Downtime downtime = analyze(DowntimeService.UNKNOWN, List.of(), 60);
        assertEquals(1, downtime.segments().size());
        assertEquals("Veri yok", downtime.segments().get(0).label());
        assertEquals(12.5, downtime.segments().get(0).widthPercent(), 1e-9);
        assertEquals(0, downtime.stopCount());
        assertNull(downtime.availability());
    }

    @Test
    @DisplayName("RUN → 10. dk STOP → 13. dk RUN, şimdi 60. dk: çalışma 57 dk, 1 duruş (3 dk), %95; genişlikler döneme göre")
    void runStopRun() {
        Downtime downtime = analyze("RUN", List.of(at(10, "STOP"), at(13, "RUN")), 60);
        assertEquals(Duration.ofMinutes(57), downtime.run());
        assertEquals(Duration.ofMinutes(3), downtime.stop());
        assertEquals(1, downtime.stopCount());
        assertEquals(Duration.ofMinutes(3), downtime.longestStop());
        assertEquals(95, downtime.availability());
        assertEquals(List.of("Çalışıyor", "Duruş", "Çalışıyor"), downtime.segments().stream().map(StatusSegment::label).toList());
        assertEquals(100.0 * 3 / 480, downtime.segments().get(1).widthPercent(), 1e-9);
    }

    @Test
    @DisplayName("Dönemden önce başlayan duruş sayılır; şimdiden sonraki kayıt hesaba girmez")
    void stopFromBeforePeriodAndClipAtNow() {
        Downtime downtime = analyze("STOP", List.of(at(5, "RUN"), at(120, "STOP")), 60);
        assertEquals(Duration.ofMinutes(5), downtime.stop());
        assertEquals(1, downtime.stopCount());
        assertEquals(Duration.ofMinutes(55), downtime.run());
        assertEquals(92, downtime.availability());
    }

    @Test
    @DisplayName("Toplayıcının tekrar yazdığı aynı durum yeni parça açmaz; dönemin ilk anındaki kayıt 'Veri yok' bırakmaz")
    void repeatedStatusIsMerged() {
        Downtime downtime = analyze(DowntimeService.UNKNOWN,
                List.of(at(0, "RUN"), at(2, "RUN"), at(4, "STOP"), at(5, "STOP"), at(6, "RUN")), 10);
        assertEquals(List.of("Çalışıyor", "Duruş", "Çalışıyor"), downtime.segments().stream().map(StatusSegment::label).toList());
        assertEquals(1, downtime.stopCount());
        assertEquals(Duration.ofMinutes(2), downtime.stop());
    }

    @Test
    @DisplayName("PASİF (planlı kapanış) kullanılabilirliğin paydasına girmez")
    void passiveIsNotDowntime() {
        Downtime downtime = analyze("RUN", List.of(at(10, "PASSIVE"), at(40, "RUN")), 60);
        assertEquals(Duration.ofMinutes(30), downtime.run());
        assertEquals(Duration.ZERO, downtime.stop());
        assertEquals(100, downtime.availability());
    }

    @Test
    @DisplayName("En uzun 5 duruş uzundan kısaya; en uzun duruş özeti ile aynı")
    void longestStopsAreTopFive() {
        List<StatusChange> changes = new ArrayList<>();
        int minute = 0;
        for (int stopMinutes = 1; stopMinutes <= 7; stopMinutes++) {
            changes.add(at(minute += 10, "STOP"));
            changes.add(at(minute += stopMinutes, "RUN"));
        }
        Downtime downtime = analyze("RUN", changes, 200);
        assertEquals(7, downtime.stopCount());
        assertEquals(Duration.ofMinutes(7), downtime.longestStop());
        assertEquals(List.of(7L, 6L, 5L, 4L, 3L), downtime.longestStops().stream().map(s -> s.duration().toMinutes()).toList());
    }

    @Test
    @DisplayName("Henüz başlamamış dönem: parça yok, kullanılabilirlik yok")
    void periodInTheFuture() {
        Downtime downtime = analyze("RUN", List.of(), -30);
        assertTrue(downtime.segments().isEmpty());
        assertNull(downtime.availability());
    }
}
