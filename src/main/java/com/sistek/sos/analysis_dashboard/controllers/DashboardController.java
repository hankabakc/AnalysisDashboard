package com.sistek.sos.analysis_dashboard.controllers;

import com.sistek.sos.analysis_dashboard.config.RefreshSettings;
import com.sistek.sos.analysis_dashboard.services.LineService;
import com.sistek.sos.analysis_dashboard.services.PlcService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final PlcService plcService;
    private final LineService lineService;
    private final RefreshSettings refreshSettings;

    public DashboardController(PlcService plcService, LineService lineService, RefreshSettings refreshSettings) {
        this.plcService = plcService;
        this.lineService = lineService;
        this.refreshSettings = refreshSettings;
    }

    @GetMapping("/dashboard")
    public String dashboardPage(Model model) {
        addDashboardData(model);
        model.addAttribute("refresh", refreshSettings);
        return "dashboard";
    }

    /** htmx'in periyodik olarak çektiği veri alanı: sayfadaki fragment'in aynısı, menü ve iskelet olmadan. */
    @GetMapping("/fragments/dashboard")
    public String dashboardFragment(Model model) {
        addDashboardData(model);
        return "fragments/dashboard-data :: dashboardData";
    }

    private void addDashboardData(Model model) {
        // Şablon PLC yoksa (null) "Tanımlı PLC bulunamadı" mesajı gösterir
        model.addAttribute("plc", plcService.findFirst().orElse(null));
        model.addAttribute("lines", lineService.findAll());
    }
}
