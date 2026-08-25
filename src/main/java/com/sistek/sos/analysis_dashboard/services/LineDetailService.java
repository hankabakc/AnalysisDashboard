package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.dto.BarcodeFilter;
import com.sistek.sos.analysis_dashboard.dto.BarcodeRow;
import com.sistek.sos.analysis_dashboard.dto.LineDetailView;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hat ayrıntısı servis katmanı.
 */
@Service
@Transactional(readOnly = true)
public class LineDetailService {

    private final BarcodeQueryService barcodeQueryService;

    public LineDetailService(BarcodeQueryService barcodeQueryService) {
        this.barcodeQueryService = barcodeQueryService;
    }

    public LineDetailView getLineDetail(String lineId, String barcodeQuery, String status, String sort, Integer page) {
        BarcodeFilter filter = BarcodeFilter.of(barcodeQuery, status, sort, page, null);
        Page<BarcodeRow> pageResult = barcodeQueryService.find(lineId, filter);

        long totalCount = pageResult.getTotalElements();
        long firstRowNo = totalCount == 0 ? 0 : ((long) filter.page() * filter.size()) + 1;
        long lastRowNo = totalCount == 0 ? 0 : Math.min(((long) (filter.page() + 1) * filter.size()), totalCount);

        return new LineDetailView(
                lineId,
                pageResult.getContent(),
                totalCount,
                filter.page(),
                pageResult.getTotalPages(),
                filter,
                firstRowNo,
                lastRowNo
        );
    }
}
