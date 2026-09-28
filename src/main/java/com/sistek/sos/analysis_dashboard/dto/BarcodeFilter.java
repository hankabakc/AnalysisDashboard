package com.sistek.sos.analysis_dashboard.dto;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Barkod arama, durum ve ERP hata kodu filtresi: ?barcodeQuery=&status=&errorCode=
 * Controller metoduna doğrudan parametre olarak bağlanır.
 * Boş arama metni, geçersiz durum ve geçersiz hata kodu "filtre yok" (null) demektir; hata vermez.
 */
public record BarcodeFilter(
        @Parameter(description = "Barkod arama sorgusu") String barcodeQuery,
        @Parameter(description = "Barkod durumu (NEW, SENT, ERROR)") String status,
        @Parameter(description = "ERP hata kodu (ör. ORA-20100, CONNECTION)") String errorCode
) {
    public BarcodeFilter {
        barcodeQuery = (barcodeQuery == null || barcodeQuery.isBlank()) ? null : barcodeQuery.strip();
        status = BarcodeStatus.normalize(status);
        errorCode = ErpError.normalize(errorCode);
    }
}
