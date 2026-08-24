package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.dto.BarcodeFilter;
import com.sistek.sos.analysis_dashboard.dto.BarcodeRow;
import com.sistek.sos.analysis_dashboard.repositories.BarcodeRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Barkod arama ve sayfalama işlemlerinin merkezi servis katmanı.
 */
@Service
@Transactional(readOnly = true)
public class BarcodeQueryService {

    private final BarcodeRepository barcodeRepository;

    public BarcodeQueryService(BarcodeRepository barcodeRepository) {
        this.barcodeRepository = barcodeRepository;
    }

    public Page<BarcodeRow> find(String lineId, BarcodeFilter filter) {
        String statusParam = filter.status() != null ? filter.status().name() : null;
        return barcodeRepository.search(lineId, filter.barcodeQuery(), statusParam, filter.toPageable())
                .map(p -> new BarcodeRow(p.getBarcode(), p.getLineId(), p.getCreDate(), p.getStatus()));
    }
}
