package com.sistek.sos.analysis_dashboard.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CSV hücre kaçışı (WEB-05 §4.1, §4.3). Barkod dış kaynaklıdır: Excel'de formül olarak çalışmamalı, ayraç veya tırnak
 * içeriyorsa sütunları kaydırmamalı.
 */
class BarcodeExportServiceTest {

    @Test
    @DisplayName("Formül başlatan karakterle (= + - @ sekme satır başı) başlayan hücreye ' ön eki")
    void formulaStartIsNeutralized() {
        assertEquals("'=1+1", BarcodeExportService.cell("=1+1"));
        assertEquals("'+90", BarcodeExportService.cell("+90"));
        assertEquals("'-5", BarcodeExportService.cell("-5"));
        assertEquals("'@SUM(A1)", BarcodeExportService.cell("@SUM(A1)"));
        assertEquals("'\tx", BarcodeExportService.cell("\tx"));
        assertEquals("\"'\rx\"", BarcodeExportService.cell("\rx"));
        assertEquals("GU141252500554", BarcodeExportService.cell("GU141252500554"));
        assertEquals("a=b", BarcodeExportService.cell("a=b"), "yalnızca baştaki karakter tehlikelidir");
    }

    @Test
    @DisplayName("Ayraç (;), tırnak ya da satır sonu içeren hücre tırnaklanır, içteki tırnak ikilenir; null boş hücre")
    void separatorsAndQuotesAreQuoted() {
        assertEquals("\"a;b\"", BarcodeExportService.cell("a;b"));
        assertEquals("\"a\"\"b\"", BarcodeExportService.cell("a\"b"));
        assertEquals("\"a\nb\"", BarcodeExportService.cell("a\nb"));
        assertEquals("", BarcodeExportService.cell(null));
        assertEquals("\"'=HYPERLINK(\"\"http://x\"\";\"\"tıkla\"\")\"",
                BarcodeExportService.cell("=HYPERLINK(\"http://x\";\"tıkla\")"));
    }
}
