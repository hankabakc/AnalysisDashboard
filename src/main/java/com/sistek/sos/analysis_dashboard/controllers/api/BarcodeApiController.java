package com.sistek.sos.analysis_dashboard.controllers.api;

import com.sistek.sos.analysis_dashboard.dto.BarcodeFilter;
import com.sistek.sos.analysis_dashboard.dto.BarcodeRow;
import com.sistek.sos.analysis_dashboard.dto.PageQuery;
import com.sistek.sos.analysis_dashboard.dto.api.PageResponse;
import com.sistek.sos.analysis_dashboard.services.LineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
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

    private final LineService lineService;

    public BarcodeApiController(LineService lineService) {
        this.lineService = lineService;
    }

    @Operation(summary = "Barkodları filtreli ve sayfalı olarak listeler")
    @GetMapping
    public PageResponse<BarcodeRow> getAllBarcodes(
            @Parameter(description = "Hat ID filtresi") @RequestParam(name = "lineId", required = false) String lineId,
            @ParameterObject BarcodeFilter filter,
            @ParameterObject PageQuery pageQuery) {
        return PageResponse.from(lineService.findBarcodes(lineId, filter, pageQuery));
    }
}
