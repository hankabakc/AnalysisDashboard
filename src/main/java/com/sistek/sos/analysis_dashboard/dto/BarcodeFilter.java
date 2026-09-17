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
        String query = (barcodeQuery == null || barcodeQuery.isBlank()) ? null : barcodeQuery.strip();
        return new BarcodeFilter(query, BarcodeStatus.fromString(status), PageQuery.of(page, size, sort));
    }

    public Pageable toPageable() {
        return pageQuery.toPageable("cre_date", "barcode");
    }
}
