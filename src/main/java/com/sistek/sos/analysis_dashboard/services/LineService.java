package com.sistek.sos.analysis_dashboard.services;

import com.sistek.sos.analysis_dashboard.dto.BarcodeFilter;
import com.sistek.sos.analysis_dashboard.dto.BarcodeRow;
import com.sistek.sos.analysis_dashboard.dto.BarcodeStatus;
import com.sistek.sos.analysis_dashboard.dto.ErpError;
import com.sistek.sos.analysis_dashboard.dto.ErrorCount;
import com.sistek.sos.analysis_dashboard.dto.LineSummary;
import com.sistek.sos.analysis_dashboard.dto.LogEntry;
import com.sistek.sos.analysis_dashboard.dto.PageQuery;
import com.sistek.sos.analysis_dashboard.dto.Period;
import com.sistek.sos.analysis_dashboard.exceptions.ResourceNotFoundException;
import com.sistek.sos.analysis_dashboard.repositories.BarcodeRepository;
import com.sistek.sos.analysis_dashboard.repositories.BarcodeRowProjection;
import com.sistek.sos.analysis_dashboard.repositories.LikeEscape;
import com.sistek.sos.analysis_dashboard.repositories.LineInfoRepository;
import com.sistek.sos.analysis_dashboard.repositories.LineLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

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

    /** Tüm hatların özet bilgileri ve seçilen dönemdeki barkod sayıları. */
    public List<LineSummary> findAll(Period period) {
        return lineInfoRepository.findSummaries(period.from(), period.to());
    }

    /** Tek bir hattın tüm zamanlardaki özet bilgisi (API). */
    public LineSummary findById(String lineId) {
        return lineInfoRepository.findSummary(lineId, Period.ALL.from(), Period.ALL.to())
                .orElseThrow(() -> notFound(lineId));
    }

    /** Belirli bir hatta ait barkodları filtreli ve sayfalı döner (Hat yoksa 404). */
    public Page<BarcodeRow> findLineBarcodes(String lineId, BarcodeFilter filter, Period period, PageQuery pageQuery) {
        requireExists(lineId);
        return findBarcodes(lineId, filter, period, pageQuery);
    }

    /** Genel barkod araması; lineId null ise tüm hatlarda arar. */
    public Page<BarcodeRow> findBarcodes(String lineId, BarcodeFilter filter, Period period, PageQuery pageQuery) {
        return barcodeRepository.search(lineId, LikeEscape.escape(filter.barcodeQuery()), filter.status(), filter.errorCode(),
                        period.from(), period.to(), pageQuery.toPageable("cre_date", "barcode"))
                .map(LineService::toRow);
    }

    /**
     * Dışa aktarım: hattın barkodları hat sayfasıyla aynı filtreyle, akış halinde (hepsi belleğe alınmaz).
     * Çağıran açık bir işlem içinde tüketip kapatmalıdır; hattın varlığını önceden requireExists ile doğrular.
     */
    public Stream<BarcodeRow> streamLineBarcodes(String lineId, BarcodeFilter filter, Period period) {
        return barcodeRepository.streamForExport(lineId, LikeEscape.escape(filter.barcodeQuery()), filter.status(),
                        filter.errorCode(), period.from(), period.to())
                .map(LineService::toRow);
    }

    /** Seçilen dönemde ERP'ye aktarılamayan barkodların hata türüne göre dağılımı; lineId null ise tüm hatlar. */
    public List<ErrorCount> findErrorCounts(String lineId, Period period) {
        return barcodeRepository.countErrors(lineId, period.from(), period.to()).stream()
                .map(e -> new ErrorCount(e.getCode(), ErpError.labelOf(e.getCode()), e.getCount()))
                .toList();
    }

    /** Hattın durum geçmiş loglarını sayfalı döner. */
    public Page<LogEntry> findLogs(String lineId, PageQuery pageQuery) {
        requireExists(lineId);
        return lineLogRepository.findByIdLineId(lineId, pageQuery.toPageable("procDate", "id.seqNo"))
                .map(log -> new LogEntry(log.getId().lineId(), log.getId().seqNo(), log.getProcDate(), log.getStatus()));
    }

    /** Hata sebebi yalnızca ERP'ye aktarılamamış (ERROR) barkodda anlamlıdır. */
    private static BarcodeRow toRow(BarcodeRowProjection b) {
        String error = BarcodeStatus.ERROR.name().equals(b.getStatus()) ? ErpError.labelOf(b.getErrorCode()) : null;
        return new BarcodeRow(b.getBarcode(), b.getLineId(), b.getCreDate(), b.getStatus(), error);
    }

    /** Hat yoksa 404. */
    public void requireExists(String lineId) {
        if (!lineInfoRepository.existsById(lineId)) {
            throw notFound(lineId);
        }
    }

    private static ResourceNotFoundException notFound(String lineId) {
        return new ResourceNotFoundException("Hat bulunamadı: " + lineId);
    }
}


