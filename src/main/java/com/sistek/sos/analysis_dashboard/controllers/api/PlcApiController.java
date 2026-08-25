package com.sistek.sos.analysis_dashboard.controllers.api;

import com.sistek.sos.analysis_dashboard.dto.LogEntry;
import com.sistek.sos.analysis_dashboard.dto.api.PageResponse;
import com.sistek.sos.analysis_dashboard.dto.api.PlcResponse;
import com.sistek.sos.analysis_dashboard.services.api.PlcApiService;
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
 */
@Tag(name = "PLC", description = "PLC bilgileri ve log uçları")
@RestController
@RequestMapping("/api/plc")
public class PlcApiController {

    private final PlcApiService plcApiService;

    public PlcApiController(PlcApiService plcApiService) {
        this.plcApiService = plcApiService;
    }

    @Operation(summary = "Tüm PLC listesini getirir")
    @GetMapping
    public List<PlcResponse> getAllPlcs() {
        return plcApiService.getAllPlcs();
    }

    @Operation(summary = "ID ile tek bir PLC getirir")
    @GetMapping("/{id}")
    public PlcResponse getPlcById(@Parameter(description = "PLC ID") @PathVariable("id") String id) {
        return plcApiService.getPlcById(id);
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
        return plcApiService.getPlcLogs(id, page, size, sort);
    }
}
