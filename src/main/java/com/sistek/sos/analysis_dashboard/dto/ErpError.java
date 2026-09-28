package com.sistek.sos.analysis_dashboard.dto;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * ERP'nin (IFS) barkod aktarımında döndüğü bilinen hata türleri ve ekranda gösterilen Türkçe etiketleri.
 * Kod, barcode_data.api_result metninden SQL ile ayıklanır (bkz. BarcodeRepository.ERROR_CODE).
 * Yeni bir hata türünü tanıtmak için buraya bir satır eklemek yeterlidir.
 */
public enum ErpError {
    ORDER_NOT_FOUND("ORA-20100", "İş emri bulunamadı"),
    BAD_FORMAT("ORA-20124", "Barkod formatı hatalı"),
    CONNECTION("CONNECTION", "ERP'ye ulaşılamadı");

    private static final Pattern VALID_CODE = Pattern.compile("ORA-\\d{1,9}|CONNECTION");

    private final String code;
    private final String label;

    ErpError(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /** Kodun ekrandaki etiketi. Tanınmayan kod kendisiyle gösterilir; kodu çıkarılamayan hata "Tanınmayan hata" olur. */
    public static String labelOf(String code) {
        if (code == null) {
            return "Tanınmayan hata";
        }
        return Arrays.stream(values())
                .filter(error -> error.code.equals(code))
                .map(error -> error.label)
                .findFirst()
                .orElse("Diğer hata (" + code + ")");
    }

    /** Filtre girdisi: geçerli bir hata koduysa büyük harfli hâlini (ör. "ORA-20100"), değilse null (filtre yok) döner. */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        // Locale.ROOT: Türkçe yerel ayarda "connection" → "CONNECTİON" olmasın
        String code = value.strip().toUpperCase(Locale.ROOT);
        return VALID_CODE.matcher(code).matches() ? code : null;
    }
}
