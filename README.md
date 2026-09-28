# Analysis Dashboard

PLC'lerden toplanan verilere dayanarak üretim hatlarının durumunu, hatlardan geçen ürün sayısını ve bu ürünlerin ERP'ye aktarılıp aktarılamadığını gösteren, fabrika içi kullanım için bir web panosu.

```
Bant → barkod okuyucu → PLC → PLC toplayıcı → PostgreSQL → ERP (IFS)
                                                  ↑
                                     bu pano veritabanını okur
```

Veriyi yazan ve ERP'ye gönderen sistem PLC toplayıcıdır; bu uygulama PLC tablolarını **yalnızca okur**. Kendi tablolarına (kullanıcılar, denetim kaydı) yazar. Tek istisna, yalnızca `dev` profilinde ve yerel veritabanına karşı çalışan [sahte toplayıcıdır](#5-geliştirme-veri-simülatörü).

---

## Yığın

| Katman | Teknoloji |
| :--- | :--- |
| Backend | Spring Boot 3.5.4 · Java 17 |
| Veri erişimi | Spring Data JPA · PostgreSQL |
| Şema yönetimi | Flyway (`ddl-auto=validate`) |
| Arayüz | Thymeleaf (sunucu render) · Bootstrap 5.3.3 · htmx 2.0.4 (periyodik tazeleme) — WebJars ile yerel, CDN yok |
| Test | JUnit 5 · MockMvc · Testcontainers (Docker içinde gerçek PostgreSQL) |

---

## Ekranlar

**`/dashboard`** — Üstte dönem seçimi; altında PLC kartı (kimlik, durum, son sinyal), her hat için kart ve tüm hatların ERP hata dağılımı.

- **Hat kartı:** seçilen dönemde okunan barkod sayısı ve ERP aktarım durumu (✔ aktarılan · ✖ hatalı · ⏳ bekleyen), çalışma oranı ve duruş sayısı (⏱ Çalışma %95 · 35 duruş), son barkod zamanı. Karta tıklayınca hat sayfası aynı dönemle açılır.
- **ERP hata dağılımı:** aktarılamayan barkodların sebebe göre sayısı — *İş emri bulunamadı* (ORA-20100), *Barkod formatı hatalı* (ORA-20124), *ERP'ye ulaşılamadı* (bağlantı hatası). Sebep `api_result` metninden ayıklanır; ham ERP yanıtı ekrana ve API'ye çıkmaz.
- **Kaynak sağlığı:** toplayıcıdan 60 sn'dir sinyal gelmiyorsa "PLC toplayıcıdan X süredir veri gelmiyor, durumlar son bilinen değerlerdir" uyarısı; 5 dk'dan uzun süredir `NEW` bekleyen barkod varsa "ERP'ye gönderilmeyi bekliyor" uyarısı.
- **Dönem:** *Bu vardiya* (varsayılan), *Bugün*, *Tüm zamanlar* veya belirli bir gün + vardiya. Vardiyalar 08–16, 16–00, 00–08; üretim günü 08:00'de başlar (gece saatleri önceki güne aittir). Hazır dönemler göreli çalışır: açık kalan ekran vardiya değişince kendiliğinden kayar.

**`/line/{lineId}`** — En üstte **çalışma durumu**: kullanılabilirlik (çalışma / (çalışma + duruş); planlı PASİF hariç), çalışma ve duruş süresi, duruş sayısı, en uzun duruş, renkli ve metin etiketli zaman çizelgesi, en uzun 5 duruş. Durum `line_log` değişimlerinden hesaplanır; hattın ilk kaydından önceki ve toplayıcının sustuğu (kalp atışı 60 sn'den uzun kesilen) aralıklar "Veri yok" sayılır, son durum sürmüş kabul edilmez. Analiz vardiya ya da gün için yapılır ("Tüm zamanlar" için hesaplanmaz).

Altında seçilen hattan seçilen dönemde geçen ürünlerin listesi: barkod, zaman, durum (renkli + metinli) ve hata sebebi. Üstte o hattın hata dağılımı; bir satıra tıklamak listeyi o hata türüne süzer. Sayfalama (50 kayıt), tarihe göre sıralama, barkod araması, duruma ve hata sebebine göre süzme. Dönem panodan gelir; arama, sayfalama, tazeleme ve "Panoya Dön" onu korur.

Veritabanındaki saatler **UTC**'dir; ekranda sunucunun saat diliminde gösterilir. Sunucu fabrika saatinde (Europe/Istanbul) çalıştırılmalıdır.

### Canlı tazeleme

İki ekran da sayfa yenilenmeden kendini günceller: pano PLC ve hat kartlarını, hat sayfası barkod tablosunu (seçili filtre ve sayfayla; arama kutusuna dokunulmaz). Her tazelenen alanda "Son güncelleme" saati görünür.

| Durum | Ekranda ne olur |
| :--- | :--- |
| Normal | Veri 5 sn'de bir sunucudan yeniden çekilir (`/fragments/dashboard`, `/fragments/line/{id}`) |
| Sunucu/ağ hatası | Son veri ekranda **kalır**; 15 sn yanıt gelmezse soluklaşır ve "Veriler güncel değil — N saniyedir yanıt alınamıyor" uyarısı çıkar. Deneme aralığı 5 → 10 → 20 → 40 → 60 sn diye uzar |
| Sunucu geri gelir | İlk başarılı yanıtta uyarı kaybolur, aralık 5 sn'ye döner |
| Oturum düşmüş (ör. sunucu yeniden başladı) | Sayfa yenilenir ve giriş ekranına gidilir |
| Sekme arka planda | İstek atılmaz; sekmeye dönünce hemen tazelenir |

Ayarlar `application.properties` içindedir; aralığı düşürmeden önce ekran sayısıyla çarpılan yükü hesaplayın (5 sn × 20 ekran = saniyede 4 istek):

```properties
dashboard.refresh.interval=5s
dashboard.refresh.stale-after-intervals=3
# Kaynak sağlığı eşikleri (toplayıcı ~5 sn'de bir sinyal yazar)
dashboard.source.silent-after=60s
dashboard.source.stuck-after=5m
# Vardiya başlangıçları (yerel saat, HH:mm); ilki üretim gününün başıdır
dashboard.shift.starts=08:00,16:00,00:00
```

İstemci tarafı `src/main/resources/static/js/refresh.js`, sunucu tarafı `config/RefreshSettings.java` ve `templates/fragments/` altındadır.

---

## API

Uygulama, veritabanındaki 5 tabloyu dışarıya sunan 8 adet salt okunur JSON REST API ucuna sahiptir.

### Swagger UI (Etkileşimli Dokümantasyon)
- **Arayüz:** <http://localhost:8080/swagger-ui.html> (veya <http://localhost:8080/swagger-ui/index.html>)
- **OpenAPI JSON:** <http://localhost:8080/v3/api-docs>

### Uç Noktaları

| Uç | Yöntem | Açıklama | Yanıt |
| :--- | :--- | :--- | :--- |
| `/api/plc` | GET | Tanımlı tüm PLC listesi (durum, son sinyal `lastSignalAt`) | `List<PlcSummary>` |
| `/api/plc/{id}` | GET | Tek bir PLC bilgisi | `PlcSummary` (404 ProblemDetail) |
| `/api/plc/{id}/logs` | GET | PLC durum değişiklik geçmişi | `PageResponse<LogEntry>` (200 / 404) |
| `/api/lines` | GET | Tüm hatlar: toplam barkod (`quantity`), ERP'ye aktarılan (`sent`), hatalı (`error`), bekleyen (`pending`), son barkod (`lastBarcodeAt`) | `List<LineSummary>` |
| `/api/lines/{id}` | GET | Tek bir hat bilgisi ve adetleri | `LineSummary` (404 ProblemDetail) |
| `/api/lines/{id}/barcodes` | GET | Hatta ait barkod listesi | `PageResponse<BarcodeRow>` (404 ProblemDetail) |
| `/api/lines/{id}/logs` | GET | Hat durum değişiklik geçmişi | `PageResponse<LogEntry>` (200 / 404) |
| `/api/barcodes` | GET | Genel barkod arama ve sayfalama | `PageResponse<BarcodeRow>` |

### Sorgu Parametreleri

| Parametre | Tip | Varsayılan | Açıklama |
| :--- | :--- | :--- | :--- |
| `page` | Integer | `0` | 0 tabanlı sayfa numarası (negatif değerler 0 kabul edilir) |
| `size` | Integer | `50` | Sayfa boyutu (1 ile 200 arası sınırlandırılır) |
| `sort` | String | `desc` | Sıralama yönü (`asc` veya `desc`) |
| `barcodeQuery` | String | — | Barkod metni arama filtresi (büyük/küçük harf duyarsız) |
| `status` | String | — | Durum filtresi (Barkod için `NEW`, `SENT`, `ERROR`) |
| `errorCode` | String | — | ERP hata kodu filtresi (`ORA-20100`, `ORA-20124`, `CONNECTION`); barkod satırında `error` alanı hata sebebinin etiketidir |
| `lineId` | String | — | `/api/barcodes` için hat filtresi |

API her zaman **tüm zamanları** döndürür (panodaki dönem seçimi API'ye uygulanmaz). Zaman alanları ofsetsiz **UTC**'dir.

---

## Güvenlik

Uygulama, Spring Security ile korunmakta olup rol bazlı erişim denetimi (RBAC) uygular. Kullanıcılar ve rolleri veritabanında (`app_user` ve `app_user_role`) saklanır; parolalar BCrypt ile karma hale getirilir.

### Roller ve Yetki Matrisi

| Rol | Web Arayüzü (`/dashboard`, `/line/**`) | REST API (`/api/**`) | Swagger & Dokümantasyon (`/swagger-ui/**`, `/v3/api-docs/**`) |
| :--- | :--- | :--- | :--- |
| `ADMIN` | ✅ Açık | ✅ Açık | ✅ Açık |
| `USER` | ✅ Açık | ❌ Kapalı | ❌ Kapalı |
| `APIUSER` | ❌ Kapalı | ✅ Açık | ❌ Kapalı |

### Geliştirme Ortamı Varsayılan Kullanıcıları

> ⚠️ **Önemli:** Bu kullanıcılar **yalnızca `dev` profiliyle ve testlerde** yüklenir (`src/main/resources/db/dev/afterMigrate__dev_users.sql`). Profil verilmeden açılan uygulamada (canlı ortam) hiçbir kullanıcı otomatik oluşmaz; `V3` migrasyonu eski sürümün eklediği bu hesapları da siler.

| Kullanıcı Adı | Parola | Rol |
| :--- | :--- | :--- |
| `admin` | `admin123` | `ADMIN` |
| `user` | `user123` | `USER` |
| `apiuser` | `apiuser123` | `APIUSER` |

### JWT ile REST API Erişimi

REST API (`/api/**`) uçları stateless ve JWT Bearer token ile korunmaktadır.

1. **İmzalama Anahtarı (`JWT_SECRET`):**
   Uygulama çalıştırılmadan önce en az 256-bit (32 karakter) uzunluğunda bir `JWT_SECRET` ortam değişkeni tanımlanmalıdır:
   ```bash
   export JWT_SECRET=<en-az-32-karakterlik-rastgele-anahtar>
   ```
   Windows PowerShell:
   ```bash
   $env:JWT_SECRET = "<en-az-32-karakterlik-rastgele-anahtar>"
   ```

2. **Giriş Yaparak Token Alma:**
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"apiuser","password":"apiuser123"}'
   ```
   Örnek Yanıt:
   ```json
   {
     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "tokenType": "Bearer",
     "expiresIn": 3600
   }
   ```

3. **Token ile API Çağrısı:**
   ```bash
   curl -H "Authorization: Bearer <token>" http://localhost:8080/api/lines
   ```

4. **Swagger UI ile Kullanım:**
   - Swagger arayüzü (`/swagger-ui.html`) `ADMIN` oturumuyla açılır.
   - Sağ üstteki **Authorize** düğmesine tıklanarak `/api/auth/login` ucundan alınan token girilir.

---

## Çalıştırma

### 1. Veritabanı

```bash
docker run --name gb-postgres -e POSTGRES_PASSWORD=<parola> -p 5435:5432 -d postgres
```

### 2. Bağlantı bilgisi

Parola ve imzalama anahtarı koda gömülü değildir, ortam değişkeninden okunur. Biri bile eksikse uygulama açılmaz:

```bash
export DB_PASSWORD=<parola>
export JWT_SECRET=<en-az-32-karakterlik-rastgele-anahtar>
```

Windows PowerShell'de:

```bash
$env:DB_PASSWORD = "<parola>"
$env:JWT_SECRET = "<en-az-32-karakterlik-rastgele-anahtar>"
```

Değiştirilebilir diğer değişkenler ve varsayılanları: `DB_PORT` (5435), `DB_USER` (postgres), `DB_NAME` (postgres).

### 3. Uygulama (şemayı Flyway kurar)

Geliştirme ortamında `dev` profiliyle çalıştır:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

İlk açılışta Flyway tabloları kurar; `dev` profili yukarıdaki geliştirme kullanıcılarını ekler. Pano: <http://localhost:8080/dashboard>

> ⚠️ `DB/DDL.sql` dosyasını **elle yükleme**. Yalnızca başvuru içindir (PLC toplayıcının şeması, `V1`'in kaynağı). Tablolar elle oluşturulursa Flyway boş olmayan şemayı görüp açılışı reddeder.

### 4. Örnek veri

Uygulama bir kez açılıp tablolar oluştuktan sonra örnek veriyi yükle:

```bash
for f in src/test/resources/db/sample-data/*.sql; do docker exec -i gb-postgres psql -U postgres -d postgres < "$f"; done
```

### 5. Geliştirme veri simülatörü

`dev` profilinde sahte bir PLC toplayıcı (`dev/CollectorSimulator`) çalışır; örnek veri yüklüyse pano canlı veriyle hareket eder:

- 5 sn'de bir PLC sinyali, hatlar ara sıra durup kalkar, çalışan hatlar `SIM…` barkodları okur
- Barkodlar birkaç saniye sonra "ERP'ye gönderilir": çoğu `SENT`, kalanı gerçekteki üç hata biçimiyle `ERROR`
- Yalnızca kendi `SIM` barkodlarını işler; elle eklenen `NEW` barkod takılı kalır ("bekliyor" uyarısını görmek için)

Üç kilitle korunur: yalnızca `dev` profili, `dashboard.simulator.enabled=true` (`application-dev.properties`) ve veritabanı adresi **localhost** değilse uygulama açılmaz. Durgun örnek veriye bakmak için ayarı `false` yapın. Sahte barkodları silmek için:

```bash
docker exec gb-postgres psql -U postgres -d postgres -c "DELETE FROM barcode_data WHERE barcode LIKE 'SIM%'"
```

### 6. Test

```bash
./mvnw test
```

Testler yalnızca **Docker** ister: her koşuda Testcontainers ile geçici bir PostgreSQL açılır, Flyway şemayı, geliştirme kullanıcılarını ve `src/test/resources/db/sample-data` altındaki örnek veriyi yükler (ör. Hat 1 → 1319 barkod). Yerel `gb-postgres` veritabanına dokunulmaz; `DB_PASSWORD` ve `JWT_SECRET` gerekmez. Testler sabit bir saatle (2025-07-31T09:43Z) ve `Europe/Istanbul` saat diliminde koşar; sonuç makineden makineye değişmez.

---

## Şema

Beş tablo — hepsi PLC toplayıcı sistem tarafından yazılır:

| Tablo | İçeriği |
| :--- | :--- |
| `plc_info` | PLC kimliği ve durumu (ACTIVE / PASSIVE / NOCOMM) |
| `line_info` | Hat kimliği, adı ve durumu (RUN / PASSIVE / STOP) |
| `barcode_data` | Hattan geçen barkodlar, zamanı ve ERP aktarım durumu (NEW → SENT / ERROR); `api_result` ERP'nin yanıtıdır |
| `plc_log`, `line_log` | Durum değişikliği kayıtları (`plc_log` aynı zamanda ~5 sn'lik kalp atışı) |

Tüm zaman sütunları saat dilimsiz `timestamp` ve **UTC**'dir. Şema yalnızca `src/main/resources/db/migration` altındaki Flyway migrasyonlarıyla değişir; ORM şema üretmez.
