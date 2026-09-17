package com.sistek.sos.analysis_dashboard.controllers;

import com.sistek.sos.analysis_dashboard.dto.BarcodeFilter;
import com.sistek.sos.analysis_dashboard.dto.PageQuery;
import com.sistek.sos.analysis_dashboard.services.LineService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Hat ayrıntısı web denetleyicisi.
 */
@Controller
public class LineController {

    private final LineService lineService;

    public LineController(LineService lineService) {
        this.lineService = lineService;
    }

    /** filter ve pageQuery sorgu parametrelerinden dolar ve şablona da aynı adla gider (bağlantılarda korunur). */
    @GetMapping("/line/{lineId}")
    public String lineDetailPage(
            @PathVariable("lineId") String lineId,
            @ModelAttribute("filter") BarcodeFilter filter,
            @ModelAttribute("pageQuery") PageQuery pageQuery,
            Model model) {
        model.addAttribute("lineId", lineId);
        model.addAttribute("barcodes", lineService.findLineBarcodes(lineId, filter, pageQuery));
        return "line";
    }
}
