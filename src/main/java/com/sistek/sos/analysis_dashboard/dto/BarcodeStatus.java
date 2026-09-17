package com.sistek.sos.analysis_dashboard.dto;

import java.util.Arrays;

/**
 * barcode_data.status sütununun alabileceği değerler (veritabanındaki CHECK kısıtıyla aynı).
 */
public enum BarcodeStatus {
    NEW,
    SENT,
    ERROR;

    /** Büyük/küçük harf fark etmeksizin geçerli bir durumsa adını (ör. "ERROR"), değilse null döner. */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
                .map(Enum::name)
                .filter(name -> name.equalsIgnoreCase(value.strip()))
                .findFirst()
                .orElse(null);
    }
}
