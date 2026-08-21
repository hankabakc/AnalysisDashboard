package com.sistek.sos.analysis_dashboard.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kök rota yönlendirme testi (WEB-08 §1.1).
 */
@WebMvcTest(IndexController.class)
class IndexControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("GET /: Doğrudan /dashboard adresine yönlendirme (3xx redirect) yapar")
    void kokRotaDashboardaYonlendirir() throws Exception {
        mvc.perform(get("/"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/dashboard"));
    }
}
