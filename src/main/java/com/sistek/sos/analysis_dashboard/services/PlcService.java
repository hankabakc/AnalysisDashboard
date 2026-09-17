package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.dto.LogEntry;
import com.sistek.sos.analysis_dashboard.dto.PageQuery;
import com.sistek.sos.analysis_dashboard.dto.PlcSummary;
import com.sistek.sos.analysis_dashboard.entities.PlcInfo;
import com.sistek.sos.analysis_dashboard.exceptions.ResourceNotFoundException;
import com.sistek.sos.analysis_dashboard.repositories.PlcInfoRepository;
import com.sistek.sos.analysis_dashboard.repositories.PlcLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * PLC bilgileri ve PLC durum geçmişi. plc_ip hiçbir dönüşte yer almaz.
 */
@Service
@Transactional(readOnly = true)
public class PlcService {

    private final PlcInfoRepository plcInfoRepository;
    private final PlcLogRepository plcLogRepository;

    public PlcService(PlcInfoRepository plcInfoRepository, PlcLogRepository plcLogRepository) {
        this.plcInfoRepository = plcInfoRepository;
        this.plcLogRepository = plcLogRepository;
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

    private static PlcSummary toSummary(PlcInfo plc) {
        return new PlcSummary(plc.getPlcId(), plc.getStatus());
    }

    private static ResourceNotFoundException notFound(String plcId) {
        return new ResourceNotFoundException("PLC bulunamadı: " + plcId);
    }
}
