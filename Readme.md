# 📋 Aplikasi Absensi – Spring Boot

Aplikasi backend absensi pegawai berbasis **Spring Boot 3.2** yang dilengkapi:
- ✅ Deteksi **lokasi palsu (mock location)**
- 📸 **Foto absen** wajib saat masuk & pulang
- 🔐 **JWT** disimpan di **Redis** (stateless, single session per device)
- ⚡ Siap untuk **traffic tinggi** (jam masuk & pulang kantor)
- 🏢 Multi-OPD dengan koordinat kantor masing-masing

---

## 🗄️ Struktur Database

| Tabel | Keterangan |
|---|---|
| `opd` | Organisasi Perangkat Daerah (instansi), termasuk koordinat kantor |
| `users` | Data pegawai dengan device_id untuk single-device enforcement |
| `shift` | Jam kerja (masuk & pulang) per OPD |
| `waktu_kerja` | Penugasan shift per user per hari |
| `absen_masuk` | Rekaman absen masuk lengkap (lokasi, foto, status) |
| `absen_pulang` | Rekaman absen pulang + durasi kerja |

---

## 🔒 Keamanan & Deteksi Lokasi Palsu

### Mekanisme Deteksi Mock Location
1. **Flag dari Android** – App Android mengirim `isMockLocation = true` jika `isFromMockProvider()` aktif
2. **Validasi akurasi GPS** – Akurasi 0.0 atau negatif = indikasi mock
3. **Deteksi teleportasi** – Kecepatan perpindahan > 50 m/s dianggap tidak wajar
4. **Provider tidak dikenal** – Hanya `gps`, `network`, `fused`, `passive` yang dipercaya
5. **Koordinat di luar range** – Latitude/longitude tidak valid langsung ditolak

### Yang Harus Dilakukan di Android
```kotlin
// Kirim flag mock location dari LocationManager
val isMock = location.isFromMockProvider

// Kirim juga provider dan akurasi
val lokasi = LokasiRequest(
    latitude = location.latitude,
    longitude = location.longitude,
    akurasiGps = location.accuracy,
    locationProvider = location.provider,
    isMockLocation = isMock
)
```

---

## 🔐 Sistem JWT + Redis

- **Access Token**: 24 jam, disimpan di Redis dengan key `jwt:token:{userId}:{deviceId}`
- **Refresh Token**: 7 hari, untuk memperbarui access token
- **Blacklist**: Setelah logout, token lama masuk blacklist di Redis
- **Single Session**: Login baru akan menimpa token lama (satu device satu sesi)
- **Validasi berlapis**: Token harus valid secara kriptografi DAN ada di Redis

---

## 📡 API Endpoints

### Auth
```
POST   /api/v1/auth/login          # Login, dapat access + refresh token
POST   /api/v1/auth/logout         # Logout, token di-blacklist
POST   /api/v1/auth/refresh        # Refresh access token
```

### Absensi (butuh Bearer token)
```
POST   /api/v1/absensi/masuk       # Absen masuk (multipart: foto + data JSON)
POST   /api/v1/absensi/pulang      # Absen pulang (multipart: foto + data JSON)
GET    /api/v1/absensi/status/hari-ini    # Cek status absen hari ini
GET    /api/v1/absensi/riwayat/masuk     # Riwayat absen masuk
GET    /api/v1/absensi/riwayat/pulang    # Riwayat absen pulang
```

### Admin (butuh ROLE_ADMIN)
```
GET    /api/v1/admin/opd                         # Daftar OPD
POST   /api/v1/admin/opd                         # Tambah OPD
PUT    /api/v1/admin/opd/{id}                    # Update OPD
GET    /api/v1/admin/shift/{opdId}               # Daftar shift
POST   /api/v1/admin/shift                       # Tambah shift
POST   /api/v1/admin/user                        # Buat user baru
PUT    /api/v1/admin/user/{id}/nonaktif          # Nonaktifkan user
GET    /api/v1/admin/laporan/absen-masuk         # Laporan per tanggal
GET    /api/v1/admin/laporan/absen-pulang        # Laporan per tanggal
GET    /api/v1/admin/laporan/rekap-user          # Rekap per user & periode
```

---

## 📱 Cara Absen dari Android

### Request Absen Masuk (Multipart)
```
POST /api/v1/absensi/masuk
Content-Type: multipart/form-data
Authorization: Bearer {token}
X-Device-Info: Android/14 Samsung Galaxy A54

Parts:
- foto: [file gambar .jpg / .png, max 5MB]
- data: {"lokasi":{"latitude":-3.5952,"longitude":98.6722,"akurasiGps":12.5,"locationProvider":"fused","isMockLocation":false}}
```

### Response Sukses
```json
{
  "success": true,
  "message": "Absen masuk berhasil",
  "data": {
    "id": 123,
    "jenis": "MASUK",
    "waktu": "2024-06-10T07:55:00",
    "jarakDariKantor": 45.2,
    "lokasiValid": true,
    "mockLocationDetected": false,
    "status": "HADIR",
    "pesan": "Absen berhasil dicatat."
  }
}
```

---

## ⚡ Optimasi High Traffic (Jam Absen)

### Kenapa Perlu?
Jam absen masuk (07:00–08:00) dan pulang (16:00–17:00) adalah **spike traffic** yang bisa membanjiri server.

### Strategi yang Diterapkan:

1. **Redis sebagai cache & rate limiter**
    - Anti-spam: minimal 60 detik antar absen
    - Rate limit: max 10 request/menit per user
    - Lokasi terakhir disimpan di Redis (bukan DB)

2. **Connection Pool yang dioptimasi**
    - PostgreSQL: 20 koneksi max, 5 idle
    - Redis Lettuce pool: 20 max active

3. **Tomcat Thread Pool**
    - Max 200 thread, 20 spare
    - Queue 100 request

4. **Batch Insert JPA**
    - `batch_size=25`, `order_inserts=true`

5. **Async Processing**
    - Thread pool 10–50 untuk task non-blocking

6. **Database Index**
    - Index pada `(user_id, tanggal)` untuk query absen hari ini
    - Index pada `(opd_id)` untuk laporan per OPD

---

## 🚀 Cara Menjalankan

### Dengan Docker Compose (Rekomendasi)
```bash
# Clone dan masuk direktori
cd absensi

# Jalankan semua service
docker-compose up -d

# Cek log
docker-compose logs -f app
```

### Manual (Development)
```bash
# 1. Pastikan PostgreSQL dan Redis berjalan

# 2. Set environment variable
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export REDIS_HOST=localhost
export JWT_SECRET=your_secret_minimum_32_characters_here

# 3. Build dan jalankan
mvn spring-boot:run
```

---

## 🔑 Environment Variables

| Variable | Default | Keterangan |
|---|---|---|
| `DB_USERNAME` | postgres | Username PostgreSQL |
| `DB_PASSWORD` | postgres | Password PostgreSQL |
| `REDIS_HOST` | localhost | Host Redis |
| `REDIS_PORT` | 6379 | Port Redis |
| `REDIS_PASSWORD` | (kosong) | Password Redis |
| `JWT_SECRET` | (wajib diubah!) | Secret key JWT min. 32 karakter |
| `UPLOAD_DIR` | ./uploads/foto | Direktori penyimpanan foto |

---

## 📁 Struktur Project

```
src/main/java/com/absensi/
├── config/          # Konfigurasi (Security, Redis, App)
├── controller/      # REST Controller
├── dto/             # Request & Response DTO
├── entity/          # JPA Entity (tabel DB)
├── enums/           # Enum (Role, Status, Jenis)
├── exception/       # Custom exception & handler
├── filter/          # JWT Auth Filter
├── repository/      # Spring Data JPA Repository
├── service/         # Business logic & Redis service
└── util/            # JWT utility & Lokasi utility
```

---

## ⚠️ Catatan Produksi

1. **Ganti JWT_SECRET** dengan string acak minimal 32 karakter
2. **Ganti password** database dan Redis
3. Gunakan **HTTPS** (SSL/TLS) di production
4. Backup foto upload ke **object storage** (S3/MinIO) untuk skala besar
5. Pertimbangkan **load balancer** jika > 1 instance (Redis sudah stateless-ready)