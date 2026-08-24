package com.sistek.sos.analysis_dashboard.repositories;

import java.time.LocalDateTime;

/**
 * Barkod verilerini salt-okunur ve hafif bir şekilde çekmek için JPA Projeksiyon arayüzü.
 */
public interface BarcodeRowProjection {
    String getBarcode();
    String getLineId();
    LocalDateTime getCreDate();
    String getStatus();
}
