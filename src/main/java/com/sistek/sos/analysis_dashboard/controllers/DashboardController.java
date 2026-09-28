package com.sistek.sos.analysis_dashboard.controllers;

import com.sistek.sos.analysis_dashboard.config.RefreshSettings;
import com.sistek.sos.analysis_dashboard.dto.Period;
import com.sistek.sos.analysis_dashboard.dto.PeriodFilter;
import com.sistek.sos.analysis_dashboard.dto.PlcSummary;
import com.sistek.sos.analysis_dashboard.services.DowntimeService;
import com.sistek.sos.analysis_dashboard.services.LineService;
import com.sistek.sos.analysis_dashboard.services.PeriodService;
import com.sistek.sos.analysis_dashboard.services.PlcService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    private final PlcService plcService;
    private final LineService lineService;
    private final PeriodService periodService;
    private final DowntimeService downtimeService;
    private final RefreshSettings refreshSettings;

    public DashboardController(PlcService plcService, LineService lineService, PeriodService periodService,
                               DowntimeService downtimeService, RefreshSettings refreshSettings) {
        this.plcService = plcService;
        this.lineService = lineService;
        this.periodService = periodService;
        this.downtimeService = downtimeService;
        this.refreshSettings = refreshSettings;
    }

    /**
     * tv=true: sahadaki büyük ekran için kiosk görünümü (menü ve dönem seçimi yok, yazı büyük). Veri alanı ve tazeleme
     * aynıdır; tazelenen fragment görünümden bağımsız olduğu için tv parametresi tazeleme adresine taşınmaz.
     */
    @GetMapping("/dashboard")
    public String dashboardPage(@ModelAttribute("periodFilter") PeriodFilter periodFilter,
                                @RequestParam(name = "tv", defaultValue = "false") boolean tv,
                                Model model) {
        addDashboardData(periodFilter, model);
        model.addAttribute("tv", tv);
        model.addAttribute("shifts", periodService.shiftLabels());
        model.addAttribute("refresh", refreshSettings);
        return "dashboard";
    }

    /** htmx'in periyodik olarak çektiği veri alanı: sayfadaki fragment'in aynısı, menü ve iskelet olmadan. */
    @GetMapping("/fragments/dashboard")
    public String dashboardFragment(@ModelAttribute("periodFilter") PeriodFilter periodFilter, Model model) {
        addDashboardData(periodFilter, model);
        return "fragments/dashboard-data :: dashboardData";
    }

    private void addDashboardData(PeriodFilter periodFilter, Model model) {
        // Göreli dönem ("bu vardiya") her tazelemede yeniden hesaplanır; açık ekran vardiya değişince kendiliğinden kayar
        Period period = periodService.resolve(periodFilter);
        model.addAttribute("period", period);
        // Şablon PLC yoksa (null) "Tanımlı PLC bulunamadı" mesajı gösterir; sağlık kontrolü PLC varsa yapılır
        PlcSummary plc = plcService.findFirst().orElse(null);
        model.addAttribute("plc", plc);
        model.addAttribute("health", plc != null ? plcService.checkCollector(plc) : null);
        model.addAttribute("lines", lineService.findAll(period));
        // Hat kimliği → duruş analizi; dönem 1 günden uzunsa ya da hattın kaydı yoksa kartta çalışma oranı yazılmaz
        model.addAttribute("downtimes", downtimeService.analyze(period, null));
        model.addAttribute("errorCounts", lineService.findErrorCounts(null, period));
    }
}
