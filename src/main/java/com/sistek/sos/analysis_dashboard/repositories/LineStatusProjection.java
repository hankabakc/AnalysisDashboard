package com.sistek.sos.analysis_dashboard.repositories;

import java.time.LocalDateTime;

/**
 * line_log'dan bir durum kaydı (duruş analizi için): hat, zaman (UTC) ve durum.
 */
public interface LineStatusProjection {
    String getLineId();
    LocalDateTime getProcDate();
    String getStatus();
}
