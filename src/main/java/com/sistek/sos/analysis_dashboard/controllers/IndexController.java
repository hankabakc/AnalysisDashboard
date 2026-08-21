package com.sistek.sos.analysis_dashboard.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Kök rota denetleyicisi.
 * Doğrudan genel üretim panosuna (/dashboard) yönlendirme yapar.
 */
@Controller
public class IndexController {

    @GetMapping("/")
    public String indexPage() {
        return "redirect:/dashboard";
    }
}
