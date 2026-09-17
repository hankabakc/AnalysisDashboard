# Analysis Dashboard

PLC'lerden toplanan verilere dayanarak üretim hatlarının durumunu ve hatlardan geçen ürün sayısını gösteren, fabrika içi kullanım için salt okunur bir web panosu.

Veriyi yazan sistem PLC toplayıcıdır; bu uygulama **yalnızca okur**, hiçbir tabloya yazmaz.

---

## Yığın

| Katman | Teknoloji |
| :--- | :--- |
| Backend | Spring Boot 3.5.4 · Java 17 |
| Veri erişimi | Spring Data JPA · PostgreSQL |
| Şema yönetimi | Flyway (`ddl-auto=validate`) |
| Arayüz | Thymeleaf (sunucu render) · Bootstrap 5.3.3 — WebJars ile yerel, CDN yok |
| Test | JUnit 5 · MockMvc · Testcontainers (Docker içinde gerçek PostgreSQL) |

---

## Ekranlar

**`/dashboard`** — PLC'nin kimliği ve durumu; altında her üretim hattının kimliği, durumu ve o hattan geçen toplam ürün adedi.

**`/line/{lineId}`** — Seçilen hattan geçen ürünlerin listesi: barkod, zaman ve durum. Sayfalama (50 kayıt), tarihe göre artan/azalan sıralama, barkoda göre arama ve duruma göre (NEW / SENT / ERROR) süzme.

---

## API

Uygulama, veritabanındaki 5 tabloyu dışarıya sunan 8 adet salt okunur JSON REST API ucuna sahiptir.

### Swagger UI (Etkileşimli Dokümantasyon)
- **Arayüz:** <http://localhost:8080/swagger-ui.html> (veya <http://localhost:8080/swagger-ui/index.html>)
- **OpenAPI JSON:** <http://localhost:8080/v3/api-docs>

### Uç Noktaları

| Uç | Yöntem | Açıklama | Yanıt |
| :--- | :--- | :--- | :--- |
| `/api/plc` | GET | Tanımlı tüm PLC listesi | `List<PlcSummary>` |
| `/api/plc/{id}` | GET | Tek bir PLC bilgisi | `PlcSummary` (404 ProblemDetail) |
| `/api/plc/{id}/logs` | GET | PLC durum değişiklik geçmişi | `PageResponse<LogEntry>` (200 / 404) |
| `/api/lines` | GET | Tüm hatlar ve toplam barkod adetleri | `List<LineSummary>` |
| `/api/lines/{id}` | GET | Tek bir hat bilgisi ve ürün adedi | `LineSummary` (404 ProblemDetail) |
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
| `lineId` | String | — | `/api/barcodes` için hat filtresi |

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

### 5. Test

```bash
./mvnw test
```

Testler yalnızca **Docker** ister: her koşuda Testcontainers ile geçici bir PostgreSQL açılır, Flyway şemayı, geliştirme kullanıcılarını ve `src/test/resources/db/sample-data` altındaki örnek veriyi yükler (ör. Hat 1 → 1319 barkod). Yerel `gb-postgres` veritabanına dokunulmaz; `DB_PASSWORD` ve `JWT_SECRET` gerekmez.

---

## Şema

Beş tablo — hepsi PLC toplayıcı sistem tarafından yazılır:

| Tablo | İçeriği |
| :--- | :--- |
| `plc_info` | PLC kimliği ve durumu (ACTIVE / PASSIVE / NOCOMM) |
| `line_info` | Hat kimliği, adı ve durumu (RUN / PASSIVE / STOP) |
| `barcode_data` | Hattan geçen barkodlar, zamanı ve durumu (NEW / SENT / ERROR) |
| `plc_log`, `line_log` | Durum değişikliği kayıtları |

Şema yalnızca `src/main/resources/db/migration` altındaki Flyway migrasyonlarıyla değişir; ORM şema üretmez.
