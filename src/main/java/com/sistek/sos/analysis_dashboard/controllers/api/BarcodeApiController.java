package com.sistek.sos.analysis_dashboard.controllers.api;

import com.sistek.sos.analysis_dashboard.dto.api.BarcodeResponse;
import com.sistek.sos.analysis_dashboard.dto.api.PageResponse;
import com.sistek.sos.analysis_dashboard.services.api.BarcodeApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Genel Barkod REST API Denetleyicisi (T-008).
 */
@RestController
@RequestMapping("/api/barcodes")
public class BarcodeApiController {

    private final BarcodeApiService barcodeApiService;

    public BarcodeApiController(BarcodeApiService barcodeApiService) {
        this.barcodeApiService = barcodeApiService;
    }

    @GetMapping
    public PageResponse<BarcodeResponse> getAllBarcodes(
            @RequestParam(name = "lineId", required = false) String lineId,
            @RequestParam(name = "barcodeQuery", required = false) String barcodeQuery,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        return barcodeApiService.getAllBarcodes(lineId, barcodeQuery, status, sort, page, size);
    }
}
