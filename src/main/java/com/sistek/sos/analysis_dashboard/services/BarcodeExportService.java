package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.dto.BarcodeFilter;
import com.sistek.sos.analysis_dashboard.dto.BarcodeRow;
import com.sistek.sos.analysis_dashboard.dto.Period;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Hat sayfasındaki barkod listesini CSV olarak yazar (WEB-05 §4).
 * Türkçe Excel'in doğrudan açacağı biçim: UTF-8 + BOM, ayraç ';', satır sonu CRLF. Satırlar veritabanından akış halinde
 * okunup yazılır; liste belleğe alınmaz. Ham ERP yanıtı yazılmaz, yalnızca hata etiketi (APP-01 §2.2).
 */
@Service
public class BarcodeExportService {

    private static final String SEPARATOR = ";";
    private static final String NEWLINE = "\r\n";
    private static final String BOM = "﻿";
    /** Excel bu karakterlerle başlayan hücreyi formül olarak çalıştırır (CSV injection). */
    private static final Pattern FORMULA_START = Pattern.compile("^[=+\\-@\t\r]");

    private final LineService lineService;

    public BarcodeExportService(LineService lineService) {
        this.lineService = lineService;
    }

    /** Akış bu işlem açıkken tüketilir; bitince kapanır. */
    @Transactional(readOnly = true)
    public void writeCsv(String lineId, BarcodeFilter filter, Period period, Writer writer) throws IOException {
        writer.write(BOM);
        writer.write(line("Barkod", "Hat", "Okunma zamanı", "Durum", "Hata sebebi"));
        try (Stream<BarcodeRow> rows = lineService.streamLineBarcodes(lineId, filter, period)) {
            Iterator<BarcodeRow> iterator = rows.iterator();
            while (iterator.hasNext()) {
                BarcodeRow row = iterator.next();
                writer.write(line(row.barcode(), row.lineId(), row.creDateText(), row.status(), row.error()));
            }
        }
        writer.flush();
    }

    private static String line(String... cells) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                line.append(SEPARATOR);
            }
            line.append(cell(cells[i]));
        }
        return line.append(NEWLINE).toString();
    }

    /**
     * Tek hücre: formül başlatan karakterle başlıyorsa başına ' eklenir (§4.1); ayraç, tırnak ya da satır sonu
     * içeriyorsa tırnak içine alınır ve içteki tırnak ikilenir (§4.3). Barkod dış kaynaklıdır, güvenilmez.
     */
    static String cell(String value) {
        if (value == null) {
            return "";
        }
        String safe = FORMULA_START.matcher(value).lookingAt() ? "'" + value : value;
        boolean quote = safe.contains(SEPARATOR) || safe.contains("\"") || safe.contains("\n") || safe.contains("\r");
        return quote ? "\"" + safe.replace("\"", "\"\"") + "\"" : safe;
    }
}
