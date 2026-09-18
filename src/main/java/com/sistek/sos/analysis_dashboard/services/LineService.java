package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.dto.BarcodeFilter;
import com.sistek.sos.analysis_dashboard.dto.BarcodeRow;
import com.sistek.sos.analysis_dashboard.dto.LineSummary;
import com.sistek.sos.analysis_dashboard.dto.LogEntry;
import com.sistek.sos.analysis_dashboard.dto.PageQuery;
import com.sistek.sos.analysis_dashboard.exceptions.ResourceNotFoundException;
import com.sistek.sos.analysis_dashboard.repositories.BarcodeRepository;
import com.sistek.sos.analysis_dashboard.repositories.LineInfoRepository;
import com.sistek.sos.analysis_dashboard.repositories.LineLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Hat özetleri, barkodlar ve hat loglarını yöneten iş mantığı servisi.
 */
@Service
@Transactional(readOnly = true)
public class LineService {

    private final LineInfoRepository lineInfoRepository;
    private final BarcodeRepository barcodeRepository;
    private final LineLogRepository lineLogRepository;

    public LineService(
            LineInfoRepository lineInfoRepository,
            BarcodeRepository barcodeRepository,
            LineLogRepository lineLogRepository) {
        this.lineInfoRepository = lineInfoRepository;
        this.barcodeRepository = barcodeRepository;
        this.lineLogRepository = lineLogRepository;
    }

    /** Tüm hatların özet bilgilerini ve toplam barkod sayılarını döner. */
    public List<LineSummary> findAll() {
        return lineInfoRepository.findSummaries();
    }

    /** Tek bir hattın özet bilgisini döner. */
    public LineSummary findById(String lineId) {
        return lineInfoRepository.findSummary(lineId)
                .orElseThrow(() -> notFound(lineId));
    }

    /** Belirli bir hatta ait barkodları filtreli ve sayfalı döner (Hat yoksa 404). */
    public Page<BarcodeRow> findLineBarcodes(String lineId, BarcodeFilter filter, PageQuery pageQuery) {
        requireExists(lineId);
        return findBarcodes(lineId, filter, pageQuery);
    }

    /** Genel barkod araması; lineId null ise tüm hatlarda arar. */
    public Page<BarcodeRow> findBarcodes(String lineId, BarcodeFilter filter, PageQuery pageQuery) {
        return barcodeRepository.search(lineId, filter.barcodeQuery(), filter.status(), pageQuery.toPageable("cre_date", "barcode"))
                .map(b -> new BarcodeRow(b.getBarcode(), b.getLineId(), b.getCreDate(), b.getStatus()));
    }

    /** Hattın durum geçmiş loglarını sayfalı döner. */
    public Page<LogEntry> findLogs(String lineId, PageQuery pageQuery) {
        requireExists(lineId);
        return lineLogRepository.findByIdLineId(lineId, pageQuery.toPageable("procDate", "id.seqNo"))
                .map(log -> new LogEntry(log.getId().lineId(), log.getId().seqNo(), log.getProcDate(), log.getStatus()));
    }

    private void requireExists(String lineId) {
        if (!lineInfoRepository.existsById(lineId)) {
            throw notFound(lineId);
        }
    }

    private static ResourceNotFoundException notFound(String lineId) {
        return new ResourceNotFoundException("Hat bulunamadı: " + lineId);
    }
}


