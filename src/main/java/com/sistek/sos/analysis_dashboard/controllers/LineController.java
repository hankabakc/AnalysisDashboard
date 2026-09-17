package com.sistek.sos.analysis_dashboard.controllers;

import com.sistek.sos.analysis_dashboard.dto.BarcodeFilter;
import com.sistek.sos.analysis_dashboard.services.LineService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Hat ayrıntısı web denetleyicisi.
 */
@Controller
public class LineController {

    private final LineService lineService;

    public LineController(LineService lineService) {
        this.lineService = lineService;
    }

    @GetMapping("/line/{lineId}")
    public String lineDetailPage(
            @PathVariable("lineId") String lineId,
            @RequestParam(name = "barcodeQuery", required = false) String barcodeQuery,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", required = false) Integer page,
            Model model) {
        BarcodeFilter filter = BarcodeFilter.of(barcodeQuery, status, sort, page, null);
        model.addAttribute("lineId", lineId);
        model.addAttribute("filter", filter);
        model.addAttribute("barcodes", lineService.findLineBarcodes(lineId, filter));
        return "line";
    }
}
