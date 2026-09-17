package com.sistek.sos.analysis_dashboard.dto;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Barkod arama ve durum filtresi: ?barcodeQuery=&status=
 * Controller metoduna doğrudan parametre olarak bağlanır.
 * Boş arama metni ve geçersiz durum "filtre yok" (null) demektir; hata vermez.
 */
public record BarcodeFilter(
        @Parameter(description = "Barkod arama sorgusu") String barcodeQuery,
        @Parameter(description = "Barkod durumu (NEW, SENT, ERROR)") String status
) {
    public BarcodeFilter {
        barcodeQuery = (barcodeQuery == null || barcodeQuery.isBlank()) ? null : barcodeQuery.strip();
        status = BarcodeStatus.normalize(status);
    }
}
