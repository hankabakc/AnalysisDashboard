package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.dto.LineView;
import com.sistek.sos.analysis_dashboard.dto.PlcView;
import com.sistek.sos.analysis_dashboard.entities.LineInfo;
import com.sistek.sos.analysis_dashboard.repositories.BarcodeRepository;
import com.sistek.sos.analysis_dashboard.repositories.LineBarcodeCountProjection;
import com.sistek.sos.analysis_dashboard.repositories.LineInfoRepository;
import com.sistek.sos.analysis_dashboard.repositories.PlcInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pano Servis Katmanı (SOLID - Clean Orchestration).
 * PLC bilgilerini PlcInfoRepository'den, hat bilgilerini LineInfoRepository'den,
 * ve kümülatif barkod sayılarını BarcodeRepository'den temin ederek DTO modellerini birleştirir.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final PlcInfoRepository plcInfoRepository;
    private final LineInfoRepository lineInfoRepository;
    private final BarcodeRepository barcodeRepository;

    public DashboardService(PlcInfoRepository plcInfoRepository,
                            LineInfoRepository lineInfoRepository,
                            BarcodeRepository barcodeRepository) {
        this.plcInfoRepository = plcInfoRepository;
        this.lineInfoRepository = lineInfoRepository;
        this.barcodeRepository = barcodeRepository;
    }

    public Optional<PlcView> getPlc() {
        return plcInfoRepository.findFirstByOrderByPlcIdAsc()
                .map(plc -> new PlcView(plc.getPlcId(), plc.getStatus()));
    }

    public List<LineView> getLines() {
        List<LineInfo> lines = lineInfoRepository.findAllByOrderByLineIdAsc();
        Map<String, Long> countMap = barcodeRepository.countBarcodesGroupedByLineId().stream()
                .collect(Collectors.toMap(
                        LineBarcodeCountProjection::getLineId,
                        LineBarcodeCountProjection::getQuantity
                ));

        return lines.stream()
                .map(line -> new LineView(
                        line.getLineId(),
                        line.getStatus(),
                        countMap.getOrDefault(line.getLineId(), 0L)
                ))
                .toList();
    }
}
