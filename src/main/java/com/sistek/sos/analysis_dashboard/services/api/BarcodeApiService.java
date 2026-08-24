package com.sistek.sos.analysis_dashboard.services.api;

import com.sistek.sos.analysis_dashboard.dto.BarcodeFilter;
import com.sistek.sos.analysis_dashboard.dto.api.BarcodeResponse;
import com.sistek.sos.analysis_dashboard.dto.api.PageResponse;
import com.sistek.sos.analysis_dashboard.repositories.BarcodeRepository;
import com.sistek.sos.analysis_dashboard.repositories.BarcodeRowProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Genel Barkod REST API servis katmanı (T-008).
 */
@Service
@Transactional(readOnly = true)
public class BarcodeApiService {

    private final BarcodeRepository barcodeRepository;

    public BarcodeApiService(BarcodeRepository barcodeRepository) {
        this.barcodeRepository = barcodeRepository;
    }

    public PageResponse<BarcodeResponse> getAllBarcodes(
            String lineId,
            String barcodeQuery,
            String status,
            String sort,
            Integer page,
            Integer size) {
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
