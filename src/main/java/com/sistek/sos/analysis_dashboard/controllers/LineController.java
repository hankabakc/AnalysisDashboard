package com.sistek.sos.analysis_dashboard.controllers;

import com.sistek.sos.analysis_dashboard.dto.LineDetailView;
import com.sistek.sos.analysis_dashboard.services.LineDetailService;
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

    private final LineDetailService lineDetailService;

    public LineController(LineDetailService lineDetailService) {
        this.lineDetailService = lineDetailService;
    }

    @GetMapping("/line/{lineId}")
    public String lineDetailPage(
            @PathVariable("lineId") String lineId,
            @RequestParam(name = "barcodeQuery", required = false) String barcodeQuery,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", required = false) Integer page,
            Model model) {
        LineDetailView detail = lineDetailService.getLineDetail(lineId, barcodeQuery, status, sort, page);
        model.addAttribute("detail", detail);
        return "line";
    }
}
