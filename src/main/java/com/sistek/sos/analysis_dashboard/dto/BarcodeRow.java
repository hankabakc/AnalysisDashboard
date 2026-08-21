package com.sistek.sos.analysis_dashboard.dto;

import java.time.LocalDateTime;

public record BarcodeRow(String barcode, LocalDateTime creDate, String status) {}
