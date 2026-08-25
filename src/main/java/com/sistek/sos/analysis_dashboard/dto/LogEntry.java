package com.sistek.sos.analysis_dashboard.dto;

import java.time.LocalDateTime;

/**
 * PLC ve Hat durum geçmişi için ortak REST yanıt modeli.
 */
public record LogEntry(
        String id,
        long seqNo,
        LocalDateTime procDate,
        String status
) {}
