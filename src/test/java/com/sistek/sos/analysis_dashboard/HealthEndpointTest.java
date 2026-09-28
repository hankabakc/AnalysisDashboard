package com.sistek.sos.analysis_dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BT izleme ve konteyner sağlık kontrolü için /actuator/health: herkese açık ama yalnızca durum;
 * iç ayrıntılar (veritabanı, disk) yalnızca ADMIN'e (APP-01 §2.2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class HealthEndpointTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @WithAnonymousUser
    @DisplayName("Kimliksiz: 200 ve yalnızca {status: UP}; bileşen ve veritabanı ayrıntısı yok")
    void anonymousSeesOnlyStatus() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(content().string(not(containsString("PostgreSQL"))));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("USER: yine yalnızca durum")
    void userSeesOnlyStatus() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN: bileşenler görünür, veritabanı UP")
    void adminSeesComponents() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.db.status", is("UP")));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Açılan tek uç health: diğer actuator uçları (ör. env) yok")
    void onlyHealthIsExposed() throws Exception {
        mvc.perform(get("/actuator/env"))
                .andExpect(status().is(not(200)));
    }
}
