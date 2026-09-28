package com.sistek.sos.analysis_dashboard.dto;

/**
 * Hata dağılımının bir satırı: ERP hata kodu (filtre değeri; çıkarılamadıysa null), ekran etiketi ve barkod adedi.
 */
public record ErrorCount(
        String code,
        String label,
        long count
) {}
