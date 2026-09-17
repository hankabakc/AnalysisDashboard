package com.sistek.sos.analysis_dashboard.controllers;

import com.sistek.sos.analysis_dashboard.services.LineService;
import com.sistek.sos.analysis_dashboard.services.PlcService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final PlcService plcService;
    private final LineService lineService;

    public DashboardController(PlcService plcService, LineService lineService) {
        this.plcService = plcService;
        this.lineService = lineService;
    }

    @GetMapping("/dashboard")
    public String dashboardPage(Model model) {
        model.addAttribute("plc", plcService.findFirst());
        model.addAttribute("lines", lineService.findAll());
        return "dashboard";
    }
}
