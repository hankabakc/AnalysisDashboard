package com.sistek.sos.analysis_dashboard.controllers;

import com.sistek.sos.analysis_dashboard.dto.AuditRow;
import com.sistek.sos.analysis_dashboard.dto.PageQuery;
import com.sistek.sos.analysis_dashboard.services.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Denetim kaydı ekranı denetleyicisi (T-017).
 * Yalnızca ADMIN yetkisine açıktır ve salt okunurdur.
 * Veri değiştiren veya dışa aktaran uç barındırmaz.
 */
@Controller
@RequestMapping("/admin/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Sayfalanmış ve kullanıcı adına göre filtrelenebilir denetim kaydı ekranını render eder.
     */
    @GetMapping
    public String auditLogPage(@RequestParam(name = "query", required = false) String query,
                               PageQuery pageQuery,
                               Model model) {
        Page<AuditRow> logs = auditService.getAuditLogs(query, pageQuery);

        model.addAttribute("logs", logs);
        model.addAttribute("query", query != null ? query.trim() : "");
        model.addAttribute("pageQuery", pageQuery);

        return "admin/audit";
    }
}
