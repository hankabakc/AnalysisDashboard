package com.sistek.sos.analysis_dashboard.controllers.api;

import com.sistek.sos.analysis_dashboard.dto.BarcodeFilter;
import com.sistek.sos.analysis_dashboard.dto.BarcodeRow;
import com.sistek.sos.analysis_dashboard.dto.api.PageResponse;
import com.sistek.sos.analysis_dashboard.services.BarcodeQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Genel barkod REST API denetleyicisi.
 */
@Tag(name = "Barkodlar", description = "Genel barkod arama ve listeleme uçları")
@RestController
@RequestMapping("/api/barcodes")
public class BarcodeApiController {

    private final BarcodeQueryService barcodeQueryService;

    public BarcodeApiController(BarcodeQueryService barcodeQueryService) {
        this.barcodeQueryService = barcodeQueryService;
    }

    @Operation(summary = "Barkodları filtreli ve sayfalı olarak listeler")
    @GetMapping
    public PageResponse<BarcodeRow> getAllBarcodes(
            @Parameter(description = "Hat ID filtresi") @RequestParam(name = "lineId", required = false) String lineId,
            @Parameter(description = "Barkod arama sorgusu") @RequestParam(name = "barcodeQuery", required = false) String barcodeQuery,
            @Parameter(description = "Barkod durumu (NEW, SENT, ERROR)") @RequestParam(name = "status", required = false) String status,
            @Parameter(description = "Sıralama yönü (asc/desc)") @RequestParam(name = "sort", required = false) String sort,
            @Parameter(description = "Sayfa numarası (0 tabanlı)") @RequestParam(name = "page", required = false) Integer page,
            @Parameter(description = "Sayfa boyutu (varsayılan 50, en fazla 200)") @RequestParam(name = "size", required = false) Integer size) {
        BarcodeFilter filter = BarcodeFilter.of(barcodeQuery, status, sort, page, size);
        Page<BarcodeRow> pageResult = barcodeQueryService.find(lineId, filter);
        return PageResponse.from(pageResult);
    }
}
