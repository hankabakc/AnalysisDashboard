package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.dto.LineSummary;
import com.sistek.sos.analysis_dashboard.entities.LineInfo;
import com.sistek.sos.analysis_dashboard.exceptions.ResourceNotFoundException;
import com.sistek.sos.analysis_dashboard.repositories.BarcodeRepository;
import com.sistek.sos.analysis_dashboard.repositories.LineBarcodeCountProjection;
import com.sistek.sos.analysis_dashboard.repositories.LineInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hat bilgileri ve ürün adetlerinin merkezi sorgu servisi.
 */
@Service
@Transactional(readOnly = true)
public class LineQueryService {

    private final LineInfoRepository lineInfoRepository;
    private final BarcodeRepository barcodeRepository;

    public LineQueryService(LineInfoRepository lineInfoRepository, BarcodeRepository barcodeRepository) {
        this.lineInfoRepository = lineInfoRepository;
        this.barcodeRepository = barcodeRepository;
    }

    public List<LineSummary> findAll() {
        List<LineInfo> lines = lineInfoRepository.findAllByOrderByLineIdAsc();
        Map<String, Long> countMap = barcodeRepository.countBarcodesGroupedByLineId().stream()
                .collect(Collectors.toMap(LineBarcodeCountProjection::getLineId, LineBarcodeCountProjection::getQuantity));

        return lines.stream()
                .map(line -> new LineSummary(
                        line.getLineId(),
                        line.getStatus(),
                        countMap.getOrDefault(line.getLineId(), 0L)
                ))
                .toList();
    }

    public LineSummary findById(String lineId) {
        LineInfo line = lineInfoRepository.findById(lineId)
                .orElseThrow(() -> new ResourceNotFoundException("Hat bulunamadı: " + lineId));

        long count = barcodeRepository.countByLineId(lineId);
        return new LineSummary(line.getLineId(), line.getStatus(), count);
    }

    public boolean existsById(String lineId) {
        return lineInfoRepository.existsById(lineId);
    }
}
