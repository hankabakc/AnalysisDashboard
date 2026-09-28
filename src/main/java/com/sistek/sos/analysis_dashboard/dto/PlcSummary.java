package com.sistek.sos.analysis_dashboard.dto;

import java.time.LocalDateTime;

/**
 * PLC kimliği, durumu ve toplayıcının son sinyal zamanı (UTC). Hem HTML panosu hem de REST API tarafından ortak kullanılır.
 * plc_ip bilinçli olarak yer almaz.
 */
public record PlcSummary(
        String id,
        String status,
        LocalDateTime lastSignalAt
) {}
