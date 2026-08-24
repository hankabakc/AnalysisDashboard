package com.sistek.sos.analysis_dashboard.services.api;

import com.sistek.sos.analysis_dashboard.dto.api.PlcResponse;
import com.sistek.sos.analysis_dashboard.exceptions.ResourceNotFoundException;
import com.sistek.sos.analysis_dashboard.repositories.PlcInfoRepository;
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

    public PlcApiService(PlcInfoRepository plcInfoRepository) {
        this.plcInfoRepository = plcInfoRepository;
    }

    public List<PlcResponse> getAllPlcs() {
        return plcInfoRepository.findAll().stream()
                .map(p -> new PlcResponse(p.getPlcId(), p.getStatus()))
                .toList();
    }

    public PlcResponse getPlcById(String id) {
        return plcInfoRepository.findById(id)
                .map(p -> new PlcResponse(p.getPlcId(), p.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("PLC bulunamadı: " + id));
    }
}
