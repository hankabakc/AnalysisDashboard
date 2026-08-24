package com.sistek.sos.analysis_dashboard.dto;

import java.util.List;

/**
 * Hat ayrıntı ekranı görünüm modeli.
 */
public record LineDetailView(
        String lineId,
        List<BarcodeRow> rows,
        long totalCount,
        int page,
        int totalPages,
        BarcodeFilter filter,
        long firstRowNo,
        long lastRowNo
) {}
