package com.sistek.sos.analysis_dashboard.services.api;

import com.sistek.sos.analysis_dashboard.dto.BarcodeFilter;
import com.sistek.sos.analysis_dashboard.dto.BarcodeRow;
import com.sistek.sos.analysis_dashboard.dto.api.PageResponse;
import com.sistek.sos.analysis_dashboard.services.BarcodeQueryService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Barkod API servis delegasyonu.
 */
@Service
@Transactional(readOnly = true)
public class BarcodeApiService {

    private final BarcodeQueryService barcodeQueryService;

    public BarcodeApiService(BarcodeQueryService barcodeQueryService) {
        this.barcodeQueryService = barcodeQueryService;
    }

    public PageResponse<BarcodeRow> getAllBarcodes(
            String lineId,
            String barcodeQuery,
            String status,
            String sort,
            Integer page,
            Integer size) {
        BarcodeFilter filter = BarcodeFilter.of(barcodeQuery, status, sort, page, size);
        Page<BarcodeRow> pageResult = barcodeQueryService.find(lineId, filter);
        return PageResponse.from(pageResult);
    }
}
