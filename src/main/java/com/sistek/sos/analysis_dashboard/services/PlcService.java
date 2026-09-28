package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.config.SourceSettings;
import com.sistek.sos.analysis_dashboard.dto.BarcodeStatus;
import com.sistek.sos.analysis_dashboard.dto.CollectorHealth;
import com.sistek.sos.analysis_dashboard.dto.DisplayTime;
import com.sistek.sos.analysis_dashboard.dto.LogEntry;
import com.sistek.sos.analysis_dashboard.dto.PageQuery;
import com.sistek.sos.analysis_dashboard.dto.PlcSummary;
import com.sistek.sos.analysis_dashboard.entities.PlcInfo;
import com.sistek.sos.analysis_dashboard.exceptions.ResourceNotFoundException;
import com.sistek.sos.analysis_dashboard.repositories.BarcodeRepository;
import com.sistek.sos.analysis_dashboard.repositories.PlcInfoRepository;
import com.sistek.sos.analysis_dashboard.repositories.PlcLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * PLC bilgileri, PLC durum geçmişi ve PLC toplayıcının sağlığı. plc_ip hiçbir dönüşte yer almaz.
 */
@Service
@Transactional(readOnly = true)
public class PlcService {

    private final PlcInfoRepository plcInfoRepository;
    private final PlcLogRepository plcLogRepository;
    private final BarcodeRepository barcodeRepository;
    private final SourceSettings sourceSettings;
    private final Clock clock;

    public PlcService(PlcInfoRepository plcInfoRepository, PlcLogRepository plcLogRepository,
                      BarcodeRepository barcodeRepository, SourceSettings sourceSettings, Clock clock) {
        this.plcInfoRepository = plcInfoRepository;
        this.plcLogRepository = plcLogRepository;
        this.barcodeRepository = barcodeRepository;
        this.sourceSettings = sourceSettings;
        this.clock = clock;
    }

    /** Pano tek PLC varsayar; tanımlı PLC yoksa boş döner. */
    public Optional<PlcSummary> findFirst() {
        return plcInfoRepository.findFirstByOrderByPlcIdAsc()
                .map(PlcService::toSummary);
    }

    public List<PlcSummary> findAll() {
        return plcInfoRepository.findAllByOrderByPlcIdAsc().stream()
                .map(PlcService::toSummary)
                .toList();
    }

    public PlcSummary findById(String plcId) {
        return plcInfoRepository.findById(plcId)
                .map(PlcService::toSummary)
                .orElseThrow(() -> notFound(plcId));
    }

    public Page<LogEntry> findLogs(String plcId, PageQuery pageQuery) {
        if (!plcInfoRepository.existsById(plcId)) {
            throw notFound(plcId);
        }
        return plcLogRepository.findByIdPlcId(plcId, pageQuery.toPageable("procDate", "id.seqNo"))
                .map(log -> new LogEntry(log.getId().plcId(), log.getId().seqNo(), log.getProcDate(), log.getStatus()));
    }

    /**
     * Toplayıcı hâlâ veri yazıyor mu, ERP'ye gönderim akıyor mu?
     * Karşılaştırmalar UTC yapılır: hem veritabanı saatleri hem de clock UTC'dir.
     */
    public CollectorHealth checkCollector(PlcSummary plc) {
        LocalDateTime now = LocalDateTime.now(clock);
        Duration signalAge = Duration.between(plc.lastSignalAt(), now);
        long stuckCount = barcodeRepository.countByStatusAndCreDateBefore(
                BarcodeStatus.NEW.name(), now.minus(sourceSettings.stuckAfter()));
        return new CollectorHealth(
                DisplayTime.format(plc.lastSignalAt()),
                DisplayTime.duration(signalAge),
                signalAge.compareTo(sourceSettings.silentAfter()) > 0,
                stuckCount,
                DisplayTime.duration(sourceSettings.stuckAfter()));
    }

    private static PlcSummary toSummary(PlcInfo plc) {
        return new PlcSummary(plc.getPlcId(), plc.getStatus(), plc.getLastStatusDate());
    }

    private static ResourceNotFoundException notFound(String plcId) {
        return new ResourceNotFoundException("PLC bulunamadı: " + plcId);
    }
}
