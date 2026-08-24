package com.sistek.sos.analysis_dashboard.dto;

/**
 * Barkod filtreleme, arama, sıralama ve sayfalama parametreleri görünüm modeli.
 */
public record BarcodeFilter(
        String barcodeQuery,
        BarcodeStatus status,
        String sort,
        int page,
        int size
) {
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 200;

    /**
     * Ham istek parametrelerini normalleştirerek güvenli BarcodeFilter nesnesi üretir.
     *
     * @param barcodeQuery Barkod arama sorgusu
     * @param status       Durum filtresi metni (NEW, SENT, ERROR)
     * @param sort         Sıralama yönü (asc, desc)
     * @param page         0 tabanlı sayfa numarası
     * @param size         Sayfa boyutu (1-200 arası)
     * @return Normalleştirilmiş BarcodeFilter
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
     * Varsayılan sayfa boyutu (50) ile BarcodeFilter üretir.
     */
    public static BarcodeFilter of(String barcodeQuery, String status, String sort, Integer page) {
        return of(barcodeQuery, status, sort, page, DEFAULT_SIZE);
    }
}
