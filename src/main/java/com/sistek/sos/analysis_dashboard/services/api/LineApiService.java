package com.sistek.sos.analysis_dashboard.services.api;

import com.sistek.sos.analysis_dashboard.dto.BarcodeFilter;
import com.sistek.sos.analysis_dashboard.dto.api.BarcodeResponse;
import com.sistek.sos.analysis_dashboard.dto.api.LineResponse;
import com.sistek.sos.analysis_dashboard.dto.api.PageResponse;
import com.sistek.sos.analysis_dashboard.entities.LineInfo;
import com.sistek.sos.analysis_dashboard.exceptions.ResourceNotFoundException;
import com.sistek.sos.analysis_dashboard.repositories.BarcodeRepository;
import com.sistek.sos.analysis_dashboard.repositories.BarcodeRowProjection;
import com.sistek.sos.analysis_dashboard.repositories.LineBarcodeCountProjection;
import com.sistek.sos.analysis_dashboard.repositories.LineInfoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hat ve Hat Barkodları REST API servis katmanı (T-008).
 */
@Service
@Transactional(readOnly = true)
public class LineApiService {

    private final LineInfoRepository lineInfoRepository;
    private final BarcodeRepository barcodeRepository;

    public LineApiService(LineInfoRepository lineInfoRepository, BarcodeRepository barcodeRepository) {
        this.lineInfoRepository = lineInfoRepository;
        this.barcodeRepository = barcodeRepository;
    }

    public List<LineResponse> getAllLines() {
        List<LineInfo> lines = lineInfoRepository.findAllByOrderByLineIdAsc();
        Map<String, Long> countMap = barcodeRepository.countBarcodesGroupedByLineId().stream()
                .collect(Collectors.toMap(LineBarcodeCountProjection::getLineId, LineBarcodeCountProjection::getQuantity));

        return lines.stream()
                .map(line -> new LineResponse(
                        line.getLineId(),
                        line.getStatus(),
                        countMap.getOrDefault(line.getLineId(), 0L)
                ))
                .toList();
    }

    public LineResponse getLineById(String id) {
        LineInfo line = lineInfoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hat bulunamadı: " + id));

        Map<String, Long> countMap = barcodeRepository.countBarcodesGroupedByLineId().stream()
                .collect(Collectors.toMap(LineBarcodeCountProjection::getLineId, LineBarcodeCountProjection::getQuantity));

        return new LineResponse(line.getLineId(), line.getStatus(), countMap.getOrDefault(line.getLineId(), 0L));
    }

    public PageResponse<BarcodeResponse> getLineBarcodes(
            String lineId,
            String barcodeQuery,
            String status,
            String sort,
            Integer page,
            Integer size) {
        if (!lineInfoRepository.existsById(lineId)) {
            throw new ResourceNotFoundException("Hat bulunamadı: " + lineId);
        }

        BarcodeFilter filter = BarcodeFilter.of(barcodeQuery, status, sort, page, size);
        Sort.Direction direction = "asc".equalsIgnoreCase(filter.sort()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(filter.page(), filter.size(), Sort.by(direction, "cre_date").and(Sort.by("barcode")));

        String statusParam = filter.status() != null ? filter.status().name() : null;
        Page<BarcodeRowProjection> pageResult = barcodeRepository.search(lineId, filter.barcodeQuery(), statusParam, pageable);

        return PageResponse.from(pageResult, p -> new BarcodeResponse(
                p.getBarcode(),
                p.getLineId(),
                p.getCreDate(),
                p.getStatus()
        ));
    }
}
