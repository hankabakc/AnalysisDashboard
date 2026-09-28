-- Okuma indeksleri (T-035). 1 M barkod + 1 yillik log ile olculdu; once/sonra sureleri PLANLAMA/ILERLEME.md'de.
-- Tablolar PLC toplayicinindir: CONCURRENTLY, indeks kurulurken toplayicinin yazmasini kilitlemez (islem disinda
-- calisir; Flyway bunu kendisi algilar). Yarida kalirsa INVALID indeks kalir: DROP INDEX ile silinip tekrar calistirilir.
-- Canli veritabaninda toplayici ekibiyle birlikte uygulanir (bkz. Flyway baseline notu, T-030).

-- Hat sayfasi (liste, sayim, saatlik uretim), hat ozetlerinin donem sayimi ve "son barkod" (hat basina max)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_barcode_data_line_cre_date ON public.barcode_data (line_id, cre_date);

-- Tum hatlar icin donem sorgulari: hata dagilimi, barkod sorgulama, tum hatlarin CSV'si
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_barcode_data_cre_date ON public.barcode_data (cre_date);

-- "ERP'ye gonderilmeyi bekliyor" sayimi: NEW satirlar bir avuctur, kismi indeks cok kucuk kalir
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_barcode_data_new_cre_date ON public.barcode_data (cre_date) WHERE status = 'NEW';

-- Toplayici sessizlikleri: kalp atisi 5 sn'de bir, yilda ~6,3 M satir; donem penceresi bu indeksle okunur
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_plc_log_proc_date ON public.plc_log (proc_date);

-- Donem basindaki hat durumu (DISTINCT ON line_id ... ORDER BY proc_date DESC, seq_no DESC): siralamasiz okunur
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_line_log_line_proc_date ON public.line_log (line_id, proc_date DESC, seq_no DESC);
