package com.sistek.sos.analysis_dashboard.controllers.api;

import com.sistek.sos.analysis_dashboard.dto.BarcodeFilter;
import com.sistek.sos.analysis_dashboard.dto.BarcodeRow;
import com.sistek.sos.analysis_dashboard.dto.api.PageResponse;
import com.sistek.sos.analysis_dashboard.services.BarcodeQueryService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Genel barkod REST API denetleyicisi.
 */
@RestController
@RequestMapping("/api/barcodes")
public class BarcodeApiController {

    private final BarcodeQueryService barcodeQueryService;

    public BarcodeApiController(BarcodeQueryService barcodeQueryService) {
        this.barcodeQueryService = barcodeQueryService;
    }

    @GetMapping
    public PageResponse<BarcodeRow> getAllBarcodes(
            @RequestParam(name = "lineId", required = false) String lineId,
            @RequestParam(name = "barcodeQuery", required = false) String barcodeQuery,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        BarcodeFilter filter = BarcodeFilter.of(barcodeQuery, status, sort, page, size);
        Page<BarcodeRow> pageResult = barcodeQueryService.find(lineId, filter);
        return PageResponse.from(pageResult);
    }
}
