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
| Arayüz | Thymeleaf (sunucu render) · Bootstrap 5.3.3 · htmx 2.0.4 — hepsi WebJars ile yerel, CDN yok |
| Test | JUnit 5 · MockMvc · gerçek PostgreSQL |

---

## Ekranlar

**`/dashboard`** — PLC'nin kimliği ve durumu; altında her üretim hattının kimliği, durumu ve o hattan geçen toplam ürün adedi.

**`/line/{lineId}`** — Seçilen hattan geçen ürünlerin listesi: barkod, zaman ve durum. Sayfalama (50 kayıt), tarihe göre artan/azalan sıralama, barkoda göre arama ve duruma göre (NEW / SENT / ERROR) süzme.

---

## Çalıştırma

### 1. Veritabanı

```bash
docker run --name gb-postgres -e POSTGRES_PASSWORD=<parola> -p 5435:5432 -d postgres
```

Şemayı ve örnek veriyi yükle:

```bash
docker exec -i gb-postgres psql -U postgres -d postgres < DB/DDL.sql
```

`DB/` klasöründeki diğer `.sql` dosyaları örnek veridir, aynı şekilde yüklenir.

### 2. Bağlantı bilgisi

Parola koda gömülü değildir, ortam değişkeninden okunur:

```bash
export DB_PASSWORD=<parola>
```

Windows PowerShell'de:

```bash
$env:DB_PASSWORD = "<parola>"
```

Değiştirilebilir diğer değişkenler ve varsayılanları: `DB_PORT` (5435), `DB_USER` (postgres), `DB_NAME` (postgres).

### 3. Uygulama

```bash
./mvnw spring-boot:run
```

Pano: <http://localhost:8080/dashboard>

### 4. Test

```bash
./mvnw test
```

Testler gerçek PostgreSQL üzerinde çalışır; bellekiçi veritabanı kullanılmaz. Çalıştırmadan önce veritabanının ayakta ve `DB_PASSWORD` değişkeninin tanımlı olması gerekir.

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
