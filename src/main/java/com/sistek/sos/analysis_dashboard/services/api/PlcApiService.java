package com.sistek.sos.analysis_dashboard.services.api;

import com.sistek.sos.analysis_dashboard.dto.LogEntry;
import com.sistek.sos.analysis_dashboard.dto.PageQuery;
import com.sistek.sos.analysis_dashboard.dto.api.PageResponse;
import com.sistek.sos.analysis_dashboard.dto.api.PlcResponse;
import com.sistek.sos.analysis_dashboard.exceptions.ResourceNotFoundException;
import com.sistek.sos.analysis_dashboard.repositories.PlcInfoRepository;
import com.sistek.sos.analysis_dashboard.services.LogQueryService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PLC REST API servis katmanı.
 * Not: Tek PLC varsayımı API'ye taşınmaz; tüm satırlar döner. plcIp hiçbir yanıtta yer almaz.
 */
@Service
@Transactional(readOnly = true)
public class PlcApiService {

    private final PlcInfoRepository plcInfoRepository;
    private final LogQueryService logQueryService;

    public PlcApiService(PlcInfoRepository plcInfoRepository, LogQueryService logQueryService) {
        this.plcInfoRepository = plcInfoRepository;
        this.logQueryService = logQueryService;
    }

    public List<PlcResponse> getAllPlcs() {
        return plcInfoRepository.findAllByOrderByPlcIdAsc().stream()
                .map(p -> new PlcResponse(p.getPlcId(), p.getStatus()))
                .toList();
    }

    public PlcResponse getPlcById(String id) {
        return plcInfoRepository.findById(id)
                .map(p -> new PlcResponse(p.getPlcId(), p.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("PLC bulunamadı: " + id));
    }

    public PageResponse<LogEntry> getPlcLogs(String id, Integer page, Integer size, String sort) {
        if (!plcInfoRepository.existsById(id)) {
            throw new ResourceNotFoundException("PLC bulunamadı: " + id);
        }

        PageQuery query = PageQuery.of(page, size, sort);
        Page<LogEntry> logPage = logQueryService.findPlcLogs(id, query);
        return PageResponse.from(logPage);
    }
}
