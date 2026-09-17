package com.sistek.sos.analysis_dashboard.controllers.api;

import com.sistek.sos.analysis_dashboard.dto.LogEntry;
import com.sistek.sos.analysis_dashboard.dto.PageQuery;
import com.sistek.sos.analysis_dashboard.dto.PlcSummary;
import com.sistek.sos.analysis_dashboard.dto.api.PageResponse;
import com.sistek.sos.analysis_dashboard.services.PlcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * PLC REST API denetleyicisi.
 * Not: Panodaki tek PLC varsayımı API'ye taşınmaz; tüm satırlar döner.
 */
@Tag(name = "PLC", description = "PLC bilgileri ve log uçları")
@RestController
@RequestMapping("/api/plc")
public class PlcApiController {

    private final PlcService plcService;

    public PlcApiController(PlcService plcService) {
        this.plcService = plcService;
    }

    @Operation(summary = "Tüm PLC listesini getirir")
    @GetMapping
    public List<PlcSummary> getAllPlcs() {
        return plcService.findAll();
    }

    @Operation(summary = "ID ile tek bir PLC getirir")
    @GetMapping("/{id}")
    public PlcSummary getPlcById(@Parameter(description = "PLC ID") @PathVariable("id") String id) {
        return plcService.findById(id);
    }

    @Operation(summary = "PLC durum loglarını sayfalı olarak getirir")
    @ApiResponse(responseCode = "200", description = "Log kayıtları listesi (kayıt yoksa boş liste döner)")
    @ApiResponse(responseCode = "404", description = "PLC bulunamadı")
    @GetMapping("/{id}/logs")
    public PageResponse<LogEntry> getPlcLogs(
            @Parameter(description = "PLC ID") @PathVariable("id") String id,
            @Parameter(description = "Sayfa numarası (0 tabanlı)") @RequestParam(name = "page", required = false) Integer page,
            @Parameter(description = "Sayfa boyutu (varsayılan 50, en fazla 200)") @RequestParam(name = "size", required = false) Integer size,
            @Parameter(description = "Sıralama yönü (asc/desc)") @RequestParam(name = "sort", required = false) String sort) {
        return PageResponse.from(plcService.findLogs(id, PageQuery.of(page, size, sort)));
    }
}
