package com.sistek.sos.analysis_dashboard.controllers.api;

import com.sistek.sos.analysis_dashboard.dto.BarcodeRow;
import com.sistek.sos.analysis_dashboard.dto.LineSummary;
import com.sistek.sos.analysis_dashboard.dto.LogEntry;
import com.sistek.sos.analysis_dashboard.dto.api.PageResponse;
import com.sistek.sos.analysis_dashboard.services.api.LineApiService;
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
 * Hat ve hat barkodları REST API denetleyicisi.
 */
@Tag(name = "Hatlar", description = "Hat bilgileri, barkod ve log uçları")
@RestController
@RequestMapping("/api/lines")
public class LineApiController {

    private final LineApiService lineApiService;

    public LineApiController(LineApiService lineApiService) {
        this.lineApiService = lineApiService;
    }

    @Operation(summary = "Tüm hat listesini getirir")
    @GetMapping
    public List<LineSummary> getAllLines() {
        return lineApiService.getAllLines();
    }

    @Operation(summary = "ID ile tek bir hat getirir")
    @GetMapping("/{id}")
    public LineSummary getLineById(@Parameter(description = "Hat ID") @PathVariable("id") String id) {
        return lineApiService.getLineById(id);
    }

    @Operation(summary = "Hatta ait barkodları filtreli ve sayfalı olarak getirir")
    @GetMapping("/{id}/barcodes")
    public PageResponse<BarcodeRow> getLineBarcodes(
            @Parameter(description = "Hat ID") @PathVariable("id") String id,
            @Parameter(description = "Barkod arama sorgusu") @RequestParam(name = "barcodeQuery", required = false) String barcodeQuery,
            @Parameter(description = "Barkod durumu (NEW, SENT, ERROR)") @RequestParam(name = "status", required = false) String status,
            @Parameter(description = "Sıralama yönü (asc/desc)") @RequestParam(name = "sort", required = false) String sort,
            @Parameter(description = "Sayfa numarası (0 tabanlı)") @RequestParam(name = "page", required = false) Integer page,
            @Parameter(description = "Sayfa boyutu (varsayılan 50, en fazla 200)") @RequestParam(name = "size", required = false) Integer size) {
        return lineApiService.getLineBarcodes(id, barcodeQuery, status, sort, page, size);
    }

    @Operation(summary = "Hat durum loglarını sayfalı olarak getirir")
    @ApiResponse(responseCode = "200", description = "Log kayıtları listesi (kayıt yoksa boş liste döner)")
    @ApiResponse(responseCode = "404", description = "Hat bulunamadı")
    @GetMapping("/{id}/logs")
    public PageResponse<LogEntry> getLineLogs(
            @Parameter(description = "Hat ID") @PathVariable("id") String id,
            @Parameter(description = "Sayfa numarası (0 tabanlı)") @RequestParam(name = "page", required = false) Integer page,
            @Parameter(description = "Sayfa boyutu (varsayılan 50, en fazla 200)") @RequestParam(name = "size", required = false) Integer size,
            @Parameter(description = "Sıralama yönü (asc/desc)") @RequestParam(name = "sort", required = false) String sort) {
        return lineApiService.getLineLogs(id, page, size, sort);
    }
}
