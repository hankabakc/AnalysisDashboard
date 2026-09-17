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
 * Hat özetleri, hattan geçen barkodlar ve hat durum geçmişi.
 * Hem web ekranları hem REST API bu servisi kullanır.
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

    public List<LineSummary> findAll() {
        return lineInfoRepository.findSummaries();
    }

    public LineSummary findById(String lineId) {
        return lineInfoRepository.findSummary(lineId)
                .orElseThrow(() -> notFound(lineId));
    }

    /** Tek bir hattın barkodları; hat yoksa 404. */
    public Page<BarcodeRow> findLineBarcodes(String lineId, BarcodeFilter filter) {
        requireExists(lineId);
        return findBarcodes(lineId, filter);
    }

    /** Genel barkod araması; lineId null ise tüm hatlarda arar. */
    public Page<BarcodeRow> findBarcodes(String lineId, BarcodeFilter filter) {
        String status = filter.status() != null ? filter.status().name() : null;
        return barcodeRepository.search(lineId, filter.barcodeQuery(), status, filter.toPageable())
                .map(b -> new BarcodeRow(b.getBarcode(), b.getLineId(), b.getCreDate(), b.getStatus()));
    }

    public Page<LogEntry> findLogs(String lineId, PageQuery query) {
        requireExists(lineId);
        return lineLogRepository.findByIdLineId(lineId, query.toPageable("procDate", "id.seqNo"))
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
