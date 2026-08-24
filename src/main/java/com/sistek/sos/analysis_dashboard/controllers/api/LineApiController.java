package com.sistek.sos.analysis_dashboard.controllers.api;

import com.sistek.sos.analysis_dashboard.dto.BarcodeRow;
import com.sistek.sos.analysis_dashboard.dto.LineSummary;
import com.sistek.sos.analysis_dashboard.dto.api.PageResponse;
import com.sistek.sos.analysis_dashboard.services.api.LineApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Hat ve hat barkodları REST API denetleyicisi.
 */
@RestController
@RequestMapping("/api/lines")
public class LineApiController {

    private final LineApiService lineApiService;

    public LineApiController(LineApiService lineApiService) {
        this.lineApiService = lineApiService;
    }

    @GetMapping
    public List<LineSummary> getAllLines() {
        return lineApiService.getAllLines();
    }

    @GetMapping("/{id}")
    public LineSummary getLineById(@PathVariable("id") String id) {
        return lineApiService.getLineById(id);
    }

    @GetMapping("/{id}/barcodes")
    public PageResponse<BarcodeRow> getLineBarcodes(
            @PathVariable("id") String id,
            @RequestParam(name = "barcodeQuery", required = false) String barcodeQuery,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        return lineApiService.getLineBarcodes(id, barcodeQuery, status, sort, page, size);
    }
}
