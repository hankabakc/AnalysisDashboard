package com.sistek.sos.analysis_dashboard.controllers.api;

import com.sistek.sos.analysis_dashboard.dto.api.PlcResponse;
import com.sistek.sos.analysis_dashboard.services.api.PlcApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
