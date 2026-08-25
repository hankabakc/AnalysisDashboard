package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.dto.LogEntry;
import com.sistek.sos.analysis_dashboard.dto.PageQuery;
import com.sistek.sos.analysis_dashboard.repositories.LineLogRepository;
import com.sistek.sos.analysis_dashboard.repositories.PlcLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PLC ve Hat durum geçmişi merkezi sorgu servisi.
 */
@Service
@Transactional(readOnly = true)
public class LogQueryService {

    private static final String PRIMARY_SORT_PROPERTY = "procDate";
    private static final String SECONDARY_SORT_PROPERTY = "id.seqNo";

    private final PlcLogRepository plcLogRepository;
    private final LineLogRepository lineLogRepository;

    public LogQueryService(PlcLogRepository plcLogRepository, LineLogRepository lineLogRepository) {
        this.plcLogRepository = plcLogRepository;
        this.lineLogRepository = lineLogRepository;
    }

    public Page<LogEntry> findPlcLogs(String plcId, PageQuery query) {
        return plcLogRepository.findByIdPlcId(plcId, toPageable(query))
                .map(log -> new LogEntry(log.getId().getPlcId(), log.getId().getSeqNo(), log.getProcDate(), log.getStatus()));
    }

    public Page<LogEntry> findLineLogs(String lineId, PageQuery query) {
        return lineLogRepository.findByIdLineId(lineId, toPageable(query))
                .map(log -> new LogEntry(log.getId().getLineId(), log.getId().getSeqNo(), log.getProcDate(), log.getStatus()));
    }

    private Pageable toPageable(PageQuery query) {
        return query.toPageable(PRIMARY_SORT_PROPERTY, SECONDARY_SORT_PROPERTY);
    }
}
