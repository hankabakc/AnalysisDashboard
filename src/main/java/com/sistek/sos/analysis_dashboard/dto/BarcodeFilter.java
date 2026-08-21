package com.sistek.sos.analysis_dashboard.dto;

/**
 * Barkod filtreleme, arama, sıralama ve sayfalama parametreleri görünüm modeli.
 */
public record BarcodeFilter(
        String barcodeQuery,
        BarcodeStatus status,
        String sort,
        int page
) {
    /**
     * Ham istek parametrelerini normalleştirerek güvenli BarcodeFilter nesnesi üretir.
     *
     * @param barcodeQuery Barkod arama sorgusu
     * @param status       Durum filtresi metni (NEW, SENT, ERROR)
     * @param sort         Sıralama yönü (asc, desc)
     * @param page         0 tabanlı sayfa numarası
     * @return Normalleştirilmiş BarcodeFilter
     */
    public static BarcodeFilter of(String barcodeQuery, String status, String sort, Integer page) {
        int normalizedPage = (page == null || page < 0) ? 0 : page;

        String normalizedSort = "desc";
        if (sort != null && ("asc".equalsIgnoreCase(sort.trim()) || "desc".equalsIgnoreCase(sort.trim()))) {
            normalizedSort = sort.trim().toLowerCase();
        }

        String normalizedBarcodeQuery = null;
        if (barcodeQuery != null && !barcodeQuery.trim().isEmpty()) {
            normalizedBarcodeQuery = barcodeQuery.trim();
        }

        BarcodeStatus normalizedStatus = BarcodeStatus.fromString(status);

        return new BarcodeFilter(normalizedBarcodeQuery, normalizedStatus, normalizedSort, normalizedPage);
    }
}
