/*
 * Periyodik tazeleme ve "Veriler güncel değil" uyarısı.
 *
 * - Tazelenen alan: hx-trigger="refresh" olan element. Bu betik ona belirli aralıklarla "refresh"
 *   olayı gönderir; htmx de alanın içeriğini sunucudan (hx-get) yeniden çeker.
 * - Aralık, bayatlık eşiği ve zaman aşımı sunucudan gelir (application.properties -> data-* öznitelikleri).
 * - Tarayıcıda veri tutulmaz; ekrandaki her değer son sunucu yanıtıdır. İstek başarısız olursa htmx
 *   içeriği değiştirmez: eski veri ekranda kalır ama eşik aşılınca "güncel değil" diye işaretlenir.
 */
(function () {
    const config = document.currentScript.dataset; // yalnızca betik ilk çalışırken okunabilir
    const area = document.querySelector('[hx-trigger="refresh"]');
    if (!area) {
        return;
    }

    const intervalMs = Number(config.refreshMs);
    const staleMs = Number(config.staleMs);
    const MAX_DELAY_MS = 60000;
    const banner = document.getElementById('stale-banner');
    const bannerSeconds = document.getElementById('stale-seconds');

    htmx.config.timeout = Number(config.timeoutMs);

    let failures = 0;
    let lastOkAt = Date.now(); // tarayıcının kendi saati; sunucu saatiyle karşılaştırılmaz
    let timer;

    // Art arda hatada bekleme uzar (5 -> 10 -> 20 -> 40 -> 60 sn) ki düşen sunucu ekranlar tarafından ezilmesin.
    // ±%10 rastgele sapma, çok sayıda ekranın aynı saniyede istek atmasını önler.
    function nextDelay() {
        const delay = Math.min(intervalMs * 2 ** failures, MAX_DELAY_MS);
        return delay * (0.9 + Math.random() * 0.2);
    }

    function scheduleNext() {
        clearTimeout(timer);
        timer = setTimeout(refreshNow, nextDelay());
    }

    function refreshNow() {
        if (document.hidden) { // arka plandaki sekme istek atmaz; yalnızca sonraki turu planlar
            scheduleNext();
            return;
        }
        htmx.trigger(area, 'refresh');
        // Yedek tur: yanıt (veya hata/zaman aşımı) gelince afterRequest bunu güncel sayaçla yeniden planlar.
        // Yanıt olayı hiç gelmezse de (istek düşürüldü vb.) tazeleme durmaz.
        scheduleNext();
    }

    // Sonraki tur, yanıt geldikten SONRA planlanır; böylece sunucu geri gelince bekleme hemen normale döner.
    // Başarılı yanıt sayacı sıfırlar; hata, zaman aşımı ve ağ kopması sayacı artırır.
    area.addEventListener('htmx:afterRequest', (event) => {
        if (event.detail.successful) {
            failures = 0;
            lastOkAt = Date.now();
        } else {
            failures++;
        }
        scheduleNext();
    });

    // Oturum düşmüşse (ör. sunucu yeniden başladı) sayfayı yenile: kullanıcı giriş ekranına yönlenir
    area.addEventListener('htmx:responseError', (event) => {
        if (event.detail.xhr.status === 401) {
            window.location.reload();
        }
    });

    // Sekmeye geri dönülünce beklemeden tazele
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) {
            clearTimeout(timer);
            refreshNow();
        }
    });

    scheduleNext();

    // Her saniye: son başarılı yanıttan bu yana eşik aşıldıysa uyarıyı göster, veriyi soluklaştır
    setInterval(() => {
        const elapsedMs = Date.now() - lastOkAt;
        const stale = elapsedMs > staleMs;
        banner.hidden = !stale;
        bannerSeconds.textContent = Math.floor(elapsedMs / 1000);
        area.classList.toggle('opacity-50', stale);
    }, 1000);
})();
