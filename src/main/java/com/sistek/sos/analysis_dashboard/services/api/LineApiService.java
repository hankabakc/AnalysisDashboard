package com.sistek.sos.analysis_dashboard.services.api;

import com.sistek.sos.analysis_dashboard.dto.BarcodeFilter;
import com.sistek.sos.analysis_dashboard.dto.BarcodeRow;
import com.sistek.sos.analysis_dashboard.dto.LineSummary;
import com.sistek.sos.analysis_dashboard.dto.api.PageResponse;
import com.sistek.sos.analysis_dashboard.exceptions.ResourceNotFoundException;
import com.sistek.sos.analysis_dashboard.services.BarcodeQueryService;
import com.sistek.sos.analysis_dashboard.services.LineQueryService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Hat ve hat barkodları REST API servisi.
 */
@Service
@Transactional(readOnly = true)
public class LineApiService {

    private final LineQueryService lineQueryService;
    private final BarcodeQueryService barcodeQueryService;

    public LineApiService(LineQueryService lineQueryService, BarcodeQueryService barcodeQueryService) {
        this.lineQueryService = lineQueryService;
        this.barcodeQueryService = barcodeQueryService;
    }

    public List<LineSummary> getAllLines() {
        return lineQueryService.findAll();
    }

    public LineSummary getLineById(String id) {
        return lineQueryService.findById(id);
    }

    public PageResponse<BarcodeRow> getLineBarcodes(
            String lineId,
            String barcodeQuery,
            String status,
            String sort,
            Integer page,
            Integer size) {
        if (!lineQueryService.existsById(lineId)) {
            throw new ResourceNotFoundException("Hat bulunamadı: " + lineId);
        }

        BarcodeFilter filter = BarcodeFilter.of(barcodeQuery, status, sort, page, size);
        Page<BarcodeRow> pageResult = barcodeQueryService.find(lineId, filter);
        return PageResponse.from(pageResult);
    }
}
