package com.sistek.sos.analysis_dashboard.controllers;

import com.sistek.sos.analysis_dashboard.dto.BarcodeFilter;
import com.sistek.sos.analysis_dashboard.dto.PageQuery;
import com.sistek.sos.analysis_dashboard.dto.Period;
import com.sistek.sos.analysis_dashboard.dto.PeriodFilter;
import com.sistek.sos.analysis_dashboard.services.BarcodeExportService;
import com.sistek.sos.analysis_dashboard.services.LineService;
import com.sistek.sos.analysis_dashboard.services.PeriodService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Barkod listeleri: tüm hatlarda barkod sorgulama sayfası ve CSV dışa aktarımları (tüm hatlar ya da tek hat).
 * Sorgulama "ERP'de bu ürün görünmüyor, nerede?" sorusu ve planlamaya verilecek hatalı aktarım listesi içindir;
 * sorgu ekranıdır, tazelenmez. Hat seçimi bir filtredir: bilinmeyen hat 404 değil boş listedir.
 */
@Controller
public class BarcodeController {

    private final LineService lineService;
    private final PeriodService periodService;
    private final BarcodeExportService barcodeExportService;

    public BarcodeController(LineService lineService, PeriodService periodService, BarcodeExportService barcodeExportService) {
        this.lineService = lineService;
        this.periodService = periodService;
        this.barcodeExportService = barcodeExportService;
    }

    @GetMapping("/barcodes")
    public String barcodesPage(
            @RequestParam(name = "lineId", required = false) String lineId,
            @ModelAttribute("filter") BarcodeFilter filter,
            @ModelAttribute("pageQuery") PageQuery pageQuery,
            @ModelAttribute("periodFilter") PeriodFilter periodFilter,
            Model model) {
        String selectedLine = blankToNull(lineId);
        Period period = periodService.resolve(periodFilter);
        model.addAttribute("selectedLine", selectedLine);
        model.addAttribute("period", period);
        model.addAttribute("lineIds", lineService.findLineIds());
        model.addAttribute("shifts", periodService.shiftLabels());
        model.addAttribute("barcodes", lineService.findBarcodes(selectedLine, filter, period, pageQuery));
        // Hata sebebi seçenekleri: seçilen hat ve dönemde gerçekten görülen türler
        model.addAttribute("errorCounts", lineService.findErrorCounts(selectedLine, period));
        return "barcodes";
    }

    /** Sorgulama sayfasındaki süzülmüş liste, tüm hatlar (ya da seçilen hat). */
    @GetMapping("/barcodes.csv")
    public void exportCsv(
            @RequestParam(name = "lineId", required = false) String lineId,
            @ModelAttribute("filter") BarcodeFilter filter,
            @ModelAttribute("periodFilter") PeriodFilter periodFilter,
            HttpServletResponse response) throws IOException {
        writeCsv(blankToNull(lineId), filter, resolveExportPeriod(periodFilter), "barkodlar.csv", response);
    }

    /**
     * Hat sayfasındaki süzülmüş liste. Hat, dosya başlıkları yazılmadan önce doğrulanır: yoksa 404 sayfası indirilecek
     * dosyaya dönüşmez.
     */
    @GetMapping("/line/{lineId}/barcodes.csv")
    public void exportLineCsv(
            @PathVariable("lineId") String lineId,
            @ModelAttribute("filter") BarcodeFilter filter,
            @ModelAttribute("periodFilter") PeriodFilter periodFilter,
            HttpServletResponse response) throws IOException {
        Period period = resolveExportPeriod(periodFilter);
        lineService.requireExists(lineId);
        writeCsv(lineId, filter, period, "hat-" + lineId + "-barkodlar.csv", response);
    }

    /** Dışa aktarımda dönem zorunlu ve sınırlıdır: en fazla bir gün (WEB-05 §4.2). */
    private Period resolveExportPeriod(PeriodFilter periodFilter) {
        Period period = periodService.resolve(periodFilter);
        if (!period.fitsInOneDay()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dışa aktarım için vardiya ya da gün seçin");
        }
        return period;
    }

    private void writeCsv(String lineId, BarcodeFilter filter, Period period, String fileName,
                          HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8).build().toString());
        barcodeExportService.writeCsv(lineId, filter, period, response.getWriter());
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.strip();
    }
}
