package com.sistek.sos.analysis_dashboard.repositories;

import java.time.LocalDateTime;

/**
 * Toplayıcının sustuğu bir aralık: [silentFrom, silentTo) UTC.
 */
public interface SilenceProjection {
    LocalDateTime getSilentFrom();
    LocalDateTime getSilentTo();
}
