package com.sistek.sos.analysis_dashboard.controllers.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST API Entegrasyon Testleri (T-008).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("GET /api/plc: 1 eleman, id=192.168.1.181, status=ACTIVE ve plcIp/plc_ip anahtarı yok")
    void getAllPlcs() throws Exception {
        mvc.perform(get("/api/plc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("192.168.1.181")))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")))
                .andExpect(content().string(not(containsString("plcIp"))))
                .andExpect(content().string(not(containsString("plc_ip"))));
    }

    @Test
    @DisplayName("GET /api/plc/192.168.1.181: 200 döner; GET /api/plc/yok: 404 ve application/problem+json döner")
    void getPlcById() throws Exception {
        mvc.perform(get("/api/plc/192.168.1.181"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("192.168.1.181")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        mvc.perform(get("/api/plc/yok"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.title", is("Kaynak Bulunamadı")))
                .andExpect(jsonPath("$.detail", containsString("PLC bulunamadı: yok")));
    }

    @Test
    @DisplayName("GET /api/lines: 5 eleman döner; quantity değerleri 1319, 6, 14, 2325, 2 ve yanıtta IP yok")
    void getAllLines() throws Exception {
        mvc.perform(get("/api/lines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].id", is("1")))
                .andExpect(jsonPath("$[0].quantity", is(1319)))
                .andExpect(jsonPath("$[1].id", is("2")))
                .andExpect(jsonPath("$[1].quantity", is(6)))
                .andExpect(jsonPath("$[2].id", is("3")))
                .andExpect(jsonPath("$[2].quantity", is(14)))
                .andExpect(jsonPath("$[3].id", is("4")))
                .andExpect(jsonPath("$[3].quantity", is(2325)))
                .andExpect(jsonPath("$[4].id", is("5")))
                .andExpect(jsonPath("$[4].quantity", is(2)))
                .andExpect(content().string(not(containsString("192.168"))));
    }

    @Test
    @DisplayName("GET /api/lines/3: quantity=14; GET /api/lines/99: 404 döner")
    void getLineById() throws Exception {
        mvc.perform(get("/api/lines/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("3")))
                .andExpect(jsonPath("$.quantity", is(14)));

        mvc.perform(get("/api/lines/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.title", is("Kaynak Bulunamadı")));
    }

    @Test
    @DisplayName("GET /api/lines/1/barcodes: totalElements=1319, content boyutu 50, totalPages=27")
    void getLine1Barcodes() throws Exception {
        mvc.perform(get("/api/lines/1/barcodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1319)))
                .andExpect(jsonPath("$.content", hasSize(50)))
                .andExpect(jsonPath("$.totalPages", is(27)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(50)))
                .andExpect(jsonPath("$.first", is(true)))
                .andExpect(jsonPath("$.last", is(false)));
    }

    @Test
    @DisplayName("GET /api/lines/1/barcodes?barcodeQuery=GU132: totalElements=172")
    void getLine1BarcodesWithQuery() throws Exception {
        mvc.perform(get("/api/lines/1/barcodes").param("barcodeQuery", "GU132"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(172)))
                .andExpect(jsonPath("$.content", hasSize(50)))
                .andExpect(jsonPath("$.totalPages", is(4)));
    }

    @Test
    @DisplayName("GET /api/lines/1/barcodes?status=SENT: totalElements=0, content boş")
    void getLine1BarcodesWithStatusSent() throws Exception {
        mvc.perform(get("/api/lines/1/barcodes").param("status", "SENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)))
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalPages", is(0)));
    }

    @Test
    @DisplayName("GET /api/barcodes: totalElements=3666; ?lineId=5: totalElements=2")
    void getAllBarcodes() throws Exception {
        // Tüm barkodlar
        mvc.perform(get("/api/barcodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3666)))
                .andExpect(jsonPath("$.content", hasSize(50)));

        // Hat 5'e göre süzülmüş
        mvc.perform(get("/api/barcodes").param("lineId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/lines/1/barcodes?page=-1&size=9999&sort=xyz: 200 döner, page=0, size=200, sort=desc uygulanır")
    void getLine1BarcodesInvalidParamsTolerated() throws Exception {
        mvc.perform(get("/api/lines/1/barcodes")
                        .param("page", "-1")
                        .param("size", "9999")
                        .param("sort", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(200)))
                .andExpect(jsonPath("$.content", hasSize(200)))
                .andExpect(jsonPath("$.totalElements", is(1319)))
                .andExpect(jsonPath("$.totalPages", is(7)));
    }

    @Test
    @DisplayName("GET /api/lines/1/barcodes?page=abc: 400 Bad Request ve application/problem+json döner")
    void getLine1BarcodesInvalidTypeReturnsProblemDetail() throws Exception {
        mvc.perform(get("/api/lines/1/barcodes").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    @DisplayName("GET /api/plc/192.168.1.181/logs: totalElements=1145, content=50, totalPages=23")
    void getPlcLogs() throws Exception {
        mvc.perform(get("/api/plc/192.168.1.181/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1145)))
                .andExpect(jsonPath("$.totalPages", is(23)))
                .andExpect(jsonPath("$.content", hasSize(50)))
                .andExpect(content().string(not(containsString("plcIp"))))
                .andExpect(content().string(not(containsString("plc_ip"))));
    }

    @Test
    @DisplayName("GET /api/plc/yok/logs: 404 döner")
    void getPlcLogsNotFound() throws Exception {
        mvc.perform(get("/api/plc/yok/logs"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.title", is("Kaynak Bulunamadı")));
    }

    @Test
    @DisplayName("GET /api/lines/1/logs: totalElements=72, totalPages=2; GET /api/lines/4/logs: totalElements=2")
    void getLineLogs() throws Exception {
        mvc.perform(get("/api/lines/1/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(72)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.content", hasSize(50)));

        mvc.perform(get("/api/lines/4/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/lines/2/logs: 200 döner, totalElements=0, content=[] (404 değil)")
    void getLineLogsEmptyForExistingLine() throws Exception {
        mvc.perform(get("/api/lines/2/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)))
                .andExpect(jsonPath("$.totalPages", is(0)))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/lines/99/logs: 404 döner")
    void getLineLogsNotFound() throws Exception {
        mvc.perform(get("/api/lines/99/logs"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.title", is("Kaynak Bulunamadı")));
    }

    @Test
    @DisplayName("GET /api/lines/1/logs?sort=asc vs ?sort=desc farklı ilk procDate döner")
    void getLineLogsSorting() throws Exception {
        String descResponse = mvc.perform(get("/api/lines/1/logs").param("sort", "desc"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String ascResponse = mvc.perform(get("/api/lines/1/logs").param("sort", "asc"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(descResponse, not(equalTo(ascResponse)));
    }

    @Test
    @DisplayName("GET /api/lines/1/logs?page=-1&size=9999&sort=xyz: 200 döner, page=0, size=200, desc uygulanır")
    void getLineLogsInvalidParamsTolerated() throws Exception {
        mvc.perform(get("/api/lines/1/logs")
                        .param("page", "-1")
                        .param("size", "9999")
                        .param("sort", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(200)))
                .andExpect(jsonPath("$.content", hasSize(72)))
                .andExpect(jsonPath("$.totalElements", is(72)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    @DisplayName("GET /v3/api-docs: 200 döner, 8 API yolunu içerir, ekranları ve plcIp sızdırmaz")
    void getApiDocs() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/plc']").exists())
                .andExpect(jsonPath("$.paths['/api/plc/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/plc/{id}/logs']").exists())
                .andExpect(jsonPath("$.paths['/api/lines']").exists())
                .andExpect(jsonPath("$.paths['/api/lines/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/lines/{id}/barcodes']").exists())
                .andExpect(jsonPath("$.paths['/api/lines/{id}/logs']").exists())
                .andExpect(jsonPath("$.paths['/api/barcodes']").exists())
                .andExpect(content().string(not(containsString("/dashboard"))))
                .andExpect(content().string(not(containsString("/line/{lineId}"))))
                .andExpect(content().string(not(containsString("plcIp"))))
                .andExpect(content().string(not(containsString("plc_ip"))));
    }

    @Test
    @DisplayName("GET /swagger-ui.html: 3xx yönlendirme döner")
    void getSwaggerUi() throws Exception {
        mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
