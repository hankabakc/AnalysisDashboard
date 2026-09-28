package com.sistek.sos.analysis_dashboard.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * ERP hata kodu → ekran etiketi eşlemesi ve filtre girdisi doğrulaması (veritabanı gerekmez).
 */
class ErpErrorTest {

    @Test
    @DisplayName("labelOf: bilinen kod etiketine, tanınmayan kod 'Diğer hata (KOD)', kodsuz hata 'Tanınmayan hata' olur")
    void labelOf() {
        assertEquals("İş emri bulunamadı", ErpError.labelOf("ORA-20100"));
        assertEquals("Barkod formatı hatalı", ErpError.labelOf("ORA-20124"));
        assertEquals("ERP'ye ulaşılamadı", ErpError.labelOf("CONNECTION"));
        assertEquals("Diğer hata (ORA-99999)", ErpError.labelOf("ORA-99999"));
        assertEquals("Tanınmayan hata", ErpError.labelOf(null));
    }

    @Test
    @DisplayName("normalize: ORA-<rakam> ve CONNECTION büyük harfe çekilir; geri kalan her şey null (filtre yok)")
    void normalize() {
        assertEquals("ORA-20100", ErpError.normalize(" ora-20100 "));
        assertEquals("CONNECTION", ErpError.normalize("connection"));
        assertNull(ErpError.normalize(null));
        assertNull(ErpError.normalize(""));
        assertNull(ErpError.normalize("HACK"));
        assertNull(ErpError.normalize("ORA-"));
        assertNull(ErpError.normalize("ORA-1' OR '1'='1"));
    }

    @Test
    @DisplayName("normalize: Türkçe yerel ayarda da 'connection' → 'CONNECTION' (noktalı İ'ye dönmez)")
    void normalizeIgnoresTurkishLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("CONNECTION", ErpError.normalize("connection"));
        } finally {
            Locale.setDefault(previous);
        }
    }
}
