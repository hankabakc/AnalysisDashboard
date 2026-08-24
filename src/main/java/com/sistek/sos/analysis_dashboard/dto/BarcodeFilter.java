package com.sistek.sos.analysis_dashboard.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Barkod arama, filtreleme, sıralama ve sayfalama parametreleri.
 */
public record BarcodeFilter(
        String barcodeQuery,
        BarcodeStatus status,
        String sort,
        int page,
        int size
) {
    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    /**
     * Ham istek parametrelerini normalleştirerek güvenli BarcodeFilter üretir.
     */
    public static BarcodeFilter of(String barcodeQuery, String status, String sort, Integer page, Integer size) {
        int normalizedPage = (page == null || page < 0) ? 0 : page;

        int normalizedSize = DEFAULT_SIZE;
        if (size != null) {
            if (size < 1) {
                normalizedSize = DEFAULT_SIZE;
            } else if (size > MAX_SIZE) {
                normalizedSize = MAX_SIZE;
            } else {
                normalizedSize = size;
            }
        }

        String normalizedSort = "desc";
        if (sort != null && ("asc".equalsIgnoreCase(sort.trim()) || "desc".equalsIgnoreCase(sort.trim()))) {
            normalizedSort = sort.trim().toLowerCase();
        }

        String normalizedBarcodeQuery = null;
        if (barcodeQuery != null && !barcodeQuery.trim().isEmpty()) {
            normalizedBarcodeQuery = barcodeQuery.trim();
        }

        BarcodeStatus normalizedStatus = BarcodeStatus.fromString(status);

        return new BarcodeFilter(normalizedBarcodeQuery, normalizedStatus, normalizedSort, normalizedPage, normalizedSize);
    }

    /**
     * Filtre parametrelerine göre güvenli Pageable nesnesi üretir.
     * İkincil anahtar olarak barcode kullanılır; bu sayede eşit zaman damgalarında sayfalar arası kayıt kayması önlenir.
     */
    public Pageable toPageable() {
        Sort.Direction direction = "asc".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, "cre_date").and(Sort.by("barcode")));
    }
}
