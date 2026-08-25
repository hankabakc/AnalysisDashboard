package com.sistek.sos.analysis_dashboard.controllers.api;

import com.sistek.sos.analysis_dashboard.dto.LogEntry;
import com.sistek.sos.analysis_dashboard.dto.api.PageResponse;
import com.sistek.sos.analysis_dashboard.dto.api.PlcResponse;
import com.sistek.sos.analysis_dashboard.services.api.PlcApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * PLC REST API denetleyicisi.
 */
@RestController
@RequestMapping("/api/plc")
public class PlcApiController {

    private final PlcApiService plcApiService;

    public PlcApiController(PlcApiService plcApiService) {
        this.plcApiService = plcApiService;
    }

    @GetMapping
    public List<PlcResponse> getAllPlcs() {
        return plcApiService.getAllPlcs();
    }

    @GetMapping("/{id}")
    public PlcResponse getPlcById(@PathVariable("id") String id) {
        return plcApiService.getPlcById(id);
    }

    @GetMapping("/{id}/logs")
    public PageResponse<LogEntry> getPlcLogs(
            @PathVariable("id") String id,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "sort", required = false) String sort) {
        return plcApiService.getPlcLogs(id, page, size, sort);
    }
}
