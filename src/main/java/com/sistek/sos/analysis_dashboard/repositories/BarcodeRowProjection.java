package com.sistek.sos.analysis_dashboard.repositories;

import java.time.LocalDateTime;

public interface BarcodeRowProjection {
    String getBarcode();
    LocalDateTime getCreDate();
    String getStatus();
}
