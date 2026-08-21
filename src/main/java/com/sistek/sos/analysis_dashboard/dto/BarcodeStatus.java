package com.sistek.sos.analysis_dashboard.dto;

import java.util.Arrays;

/**
 * Barkod işlem durumları enum modeli (Type-Safe Domain Status).
 */
public enum BarcodeStatus {
    NEW,
    SENT,
    ERROR;

    /**
     * Dize değerini güvenli şekilde BarcodeStatus enum sabitine dönüştürür.
     * Geçersiz veya boş değerler için null döner (filtresiz / hepsi anlamına gelir).
     *
     * @param value Durum metni
     * @return Eşleşen BarcodeStatus veya null
     */
    public static BarcodeStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElse(null);
    }
}
