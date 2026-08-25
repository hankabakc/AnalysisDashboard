package com.sistek.sos.analysis_dashboard.dto;

import org.springframework.data.domain.Pageable;

/**
 * Barkod arama, filtreleme, sıralama ve sayfalama parametreleri.
 */
public record BarcodeFilter(
        String barcodeQuery,
        BarcodeStatus status,
        PageQuery pageQuery
) {
    public static BarcodeFilter of(String barcodeQuery, String status, String sort, Integer page, Integer size) {
        String normalizedBarcodeQuery = null;
        if (barcodeQuery != null && !barcodeQuery.trim().isEmpty()) {
            normalizedBarcodeQuery = barcodeQuery.trim();
        }

        BarcodeStatus normalizedStatus = BarcodeStatus.fromString(status);
        PageQuery normalizedPageQuery = PageQuery.of(page, size, sort);

        return new BarcodeFilter(normalizedBarcodeQuery, normalizedStatus, normalizedPageQuery);
    }

    public Pageable toPageable() {
        return pageQuery.toPageable("cre_date", "barcode");
    }

    public int page() {
        return pageQuery.page();
    }

    public int size() {
        return pageQuery.size();
    }

    public String sort() {
        return pageQuery.sort();
    }
}
