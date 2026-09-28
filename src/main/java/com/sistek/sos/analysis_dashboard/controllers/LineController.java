package com.sistek.sos.analysis_dashboard.controllers;

import com.sistek.sos.analysis_dashboard.config.RefreshSettings;
import com.sistek.sos.analysis_dashboard.dto.BarcodeFilter;
import com.sistek.sos.analysis_dashboard.dto.PageQuery;
import com.sistek.sos.analysis_dashboard.dto.Period;
import com.sistek.sos.analysis_dashboard.dto.PeriodFilter;
import com.sistek.sos.analysis_dashboard.services.BarcodeExportService;
import com.sistek.sos.analysis_dashboard.services.DowntimeService;
import com.sistek.sos.analysis_dashboard.services.HourlyProductionService;
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
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class LineController {

    private final LineService lineService;
    private final PeriodService periodService;
    private final DowntimeService downtimeService;
    private final HourlyProductionService hourlyProductionService;
    private final BarcodeExportService barcodeExportService;
    private final RefreshSettings refreshSettings;

    public LineController(LineService lineService, PeriodService periodService, DowntimeService downtimeService,
                          HourlyProductionService hourlyProductionService, BarcodeExportService barcodeExportService,
                          RefreshSettings refreshSettings) {
        this.lineService = lineService;
        this.periodService = periodService;
        this.downtimeService = downtimeService;
        this.hourlyProductionService = hourlyProductionService;
        this.barcodeExportService = barcodeExportService;
        this.refreshSettings = refreshSettings;
    }

    @GetMapping("/line/{lineId}")
    public String lineDetailPage(
            @PathVariable("lineId") String lineId,
            @ModelAttribute("filter") BarcodeFilter filter,
            @ModelAttribute("pageQuery") PageQuery pageQuery,
            @ModelAttribute("periodFilter") PeriodFilter periodFilter,
            Model model) {
        addBarcodes(lineId, filter, pageQuery, periodFilter, model);
        model.addAttribute("refresh", refreshSettings);
        return "line";
    }

    @GetMapping("/fragments/line/{lineId}")
    public String barcodeTableFragment(
            @PathVariable("lineId") String lineId,
            @ModelAttribute("filter") BarcodeFilter filter,
            @ModelAttribute("pageQuery") PageQuery pageQuery,
            @ModelAttribute("periodFilter") PeriodFilter periodFilter,
            Model model) {
        addBarcodes(lineId, filter, pageQuery, periodFilter, model);
        return "fragments/barcode-table :: barcodeTable";
    }

    /**
     * Hat sayfasındaki süzülmüş listeyi CSV olarak indirir. Dönem zorunlu ve sınırlıdır (en fazla bir gün, WEB-05 §4.2).
     * Hat, dosya başlıkları yazılmadan önce doğrulanır: yoksa 404 sayfası indirilecek dosyaya dönüşmez.
     */
    @GetMapping("/line/{lineId}/barcodes.csv")
    public void exportCsv(
            @PathVariable("lineId") String lineId,
            @ModelAttribute("filter") BarcodeFilter filter,
            @ModelAttribute("periodFilter") PeriodFilter periodFilter,
            HttpServletResponse response) throws IOException {
        Period period = periodService.resolve(periodFilter);
        if (!period.fitsInOneDay()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dışa aktarım için vardiya ya da gün seçin");
        }
        lineService.requireExists(lineId);
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename("hat-" + lineId + "-barkodlar.csv", StandardCharsets.UTF_8).build().toString());
        barcodeExportService.writeCsv(lineId, filter, period, response.getWriter());
    }

    private void addBarcodes(String lineId, BarcodeFilter filter, PageQuery pageQuery, PeriodFilter periodFilter, Model model) {
        Period period = periodService.resolve(periodFilter);
        model.addAttribute("period", period);
        model.addAttribute("lineId", lineId);
        model.addAttribute("barcodes", lineService.findLineBarcodes(lineId, filter, period, pageQuery));
        model.addAttribute("errorCounts", lineService.findErrorCounts(lineId, period));
        model.addAttribute("downtime", downtimeService.analyze(period, lineId).get(lineId));
        model.addAttribute("hourly", hourlyProductionService.find(lineId, period));
    }
}
