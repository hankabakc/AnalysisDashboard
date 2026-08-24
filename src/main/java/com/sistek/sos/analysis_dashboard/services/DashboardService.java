package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.dto.LineSummary;
import com.sistek.sos.analysis_dashboard.dto.PlcView;
import com.sistek.sos.analysis_dashboard.repositories.PlcInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Pano görünüm orkestrasyon servisi.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final PlcInfoRepository plcInfoRepository;
    private final LineQueryService lineQueryService;

    public DashboardService(PlcInfoRepository plcInfoRepository, LineQueryService lineQueryService) {
        this.plcInfoRepository = plcInfoRepository;
        this.lineQueryService = lineQueryService;
    }

    public PlcView getPlc() {
        return plcInfoRepository.findFirstByOrderByPlcIdAsc()
                .map(p -> new PlcView(p.getPlcId(), p.getStatus()))
                .orElse(null);
    }

    public List<LineSummary> getLines() {
        return lineQueryService.findAll();
    }
}
