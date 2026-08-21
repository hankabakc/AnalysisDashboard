package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.dto.BarcodeFilter;
import com.sistek.sos.analysis_dashboard.dto.BarcodeRow;
import com.sistek.sos.analysis_dashboard.dto.LineDetailView;
import com.sistek.sos.analysis_dashboard.repositories.BarcodeRepository;
import com.sistek.sos.analysis_dashboard.repositories.BarcodeRowProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Hat ayrıntısı servis katmanı (T-007).
 */
@Service
@Transactional(readOnly = true)
public class LineDetailService {

    private static final int PAGE_SIZE = 50;

    private final BarcodeRepository barcodeRepository;

    public LineDetailService(BarcodeRepository barcodeRepository) {
        this.barcodeRepository = barcodeRepository;
    }

    /**
     * Hat ayrıntılarını, filtrelenmiş ve sayfalanmış barkod listesini döner.
     *
     * @param lineId       Hat kimliği
     * @param barcodeQuery Barkod arama filtresi
     * @param status       Durum filtresi
     * @param sort         Sıralama yönü (asc, desc)
     * @param page         Sayfa numarası (0 tabanlı)
     * @return Hat ayrıntı görünüm modeli
     */
    public LineDetailView getLineDetail(String lineId, String barcodeQuery, String status, String sort, Integer page) {
        BarcodeFilter filter = BarcodeFilter.of(barcodeQuery, status, sort, page);

        Sort.Direction direction = "asc".equalsIgnoreCase(filter.sort()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        // İkinci anahtar barcode: eşit zaman damgalarında sayfalar arası kayıt kaymasını önler
        Pageable pageable = PageRequest.of(filter.page(), PAGE_SIZE, Sort.by(direction, "cre_date").and(Sort.by("barcode")));

        String statusParam = filter.status() != null ? filter.status().name() : null;
        Page<BarcodeRowProjection> pageResult = barcodeRepository.search(lineId, filter.barcodeQuery(), statusParam, pageable);

        List<BarcodeRow> rows = pageResult.getContent().stream()
                .map(p -> new BarcodeRow(p.getBarcode(), p.getCreDate(), p.getStatus()))
                .toList();

        long totalCount = pageResult.getTotalElements();
        int totalPages = pageResult.getTotalPages();

        long firstRowNo = totalCount == 0 ? 0 : ((long) filter.page() * PAGE_SIZE) + 1;
        long lastRowNo = totalCount == 0 ? 0 : Math.min(((long) (filter.page() + 1) * PAGE_SIZE), totalCount);

        return new LineDetailView(
                lineId,
                rows,
                totalCount,
                filter.page(),
                totalPages,
                filter,
                firstRowNo,
                lastRowNo
        );
    }
}
