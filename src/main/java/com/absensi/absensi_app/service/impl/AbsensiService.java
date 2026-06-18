package com.absensi.absensi_app.service.impl;

import com.absensi.absensi_app.dto.request.AbsenRequest;
import com.absensi.absensi_app.dto.response.AbsenResponse;
import com.absensi.absensi_app.entity.*;
import com.absensi.absensi_app.enums.StatusAbsensi;
import com.absensi.absensi_app.exception.AbsensiException;
import com.absensi.absensi_app.repository.*;
import com.absensi.absensi_app.service.RedisTokenService;
import com.absensi.absensi_app.util.LokasiUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AbsensiService {

    private final AbsenMasukRepository absenMasukRepository;
    private final AbsenPulangRepository absenPulangRepository;
    private final WaktuKerjaRepository waktuKerjaRepository;
    private final UserRepository userRepository;
    private final LokasiUtil lokasiUtil;
    private final RedisTokenService redisTokenService;
    private final com.absensi.absensi_app.service.FotoService fotoService;

    @Value("${app.absensi.interval-minimal:60}")
    private int intervalMinimalDetik;

    /**
     * Proses absen masuk dengan validasi lokasi & foto
     */
    @Transactional
    public AbsenResponse absenMasuk(Long userId, AbsenRequest request,
                                    MultipartFile foto, String ipAddress, String deviceInfo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AbsensiException("User tidak ditemukan"));

        LocalDate today = LocalDate.now();

        // Cek sudah absen masuk hari ini
        if (absenMasukRepository.existsByUserIdAndTanggal(userId, today)) {
            throw new AbsensiException("Anda sudah melakukan absen masuk hari ini");
        }

        // Anti-spam: cek interval dari Redis
        cekIntervalAbsen(userId, "masuk");

        Opd opd = user.getOpd();

        // Validasi & deteksi lokasi palsu
        double[] lokasiSebelumnya = redisTokenService.ambilLokasiTerakhir(userId);
        LokasiUtil.HasilValidasiLokasi validasiLokasi = lokasiUtil.validasiLokasi(
                request.getLokasi(),
                lokasiSebelumnya != null ? lokasiSebelumnya[0] : null,
                lokasiSebelumnya != null ? lokasiSebelumnya[1] : null,
                lokasiSebelumnya != null ? (long) lokasiSebelumnya[2] : null
        );

        double jarak = lokasiUtil.hitungJarak(
                request.getLokasi().getLatitude(), request.getLokasi().getLongitude(),
                opd.getLatitudeKantor(), opd.getLongitudeKantor()
        );

        boolean lokasiValid = !validasiLokasi.isMockTerdeteksi()
                && lokasiUtil.isLokasiDalamRadius(
                request.getLokasi().getLatitude(), request.getLokasi().getLongitude(),
                opd.getLatitudeKantor(), opd.getLongitudeKantor(),
                opd.getRadiusAbsen()
        );

        // Simpan foto
        String pathFoto = null;
        if (foto != null && !foto.isEmpty()) {
            try {
                pathFoto = fotoService.uploadFoto(foto, userId, "masuk");
            } catch (IOException e) {
                log.error("Gagal upload foto absen masuk user {}: {}", userId, e.getMessage());
                throw new AbsensiException("Gagal menyimpan foto absen");
            }
        } else {
            throw new AbsensiException("Foto absen masuk wajib diisi");
        }

        // Cari shift aktif hari ini
        Shift shift = cariShiftAktif(userId, today);

        // Tentukan status (hadir/terlambat)
        StatusAbsensi status = tentukanStatusMasuk(shift);

        AbsenMasuk absenMasuk = AbsenMasuk.builder()
                .user(user)
                .opd(opd)
                .shift(shift)
                .tanggal(today)
                .waktuMasuk(LocalDateTime.now())
                .latitude(request.getLokasi().getLatitude())
                .longitude(request.getLokasi().getLongitude())
                .akurasiGps(request.getLokasi().getAkurasiGps())
                .jarakDariKantor(jarak)
                .fotoAbsen(pathFoto)
                .lokasiValid(lokasiValid)
                .mockLocationDetected(validasiLokasi.isMockTerdeteksi())
                .locationProvider(request.getLokasi().getLocationProvider())
                .kecepatanPerpindahan(validasiLokasi.getKecepatanPerpindahan())
                .status(status)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .catatan(request.getCatatan())
                .build();

        absenMasukRepository.save(absenMasuk);

        // Update lokasi terakhir di Redis
        redisTokenService.simpanLokasiTerakhir(userId,
                request.getLokasi().getLatitude(),
                request.getLokasi().getLongitude(),
                System.currentTimeMillis());

        // Simpan timestamp absen untuk rate limiting
        tandaiAbsen(userId, "masuk");

        log.info("Absen masuk berhasil - user: {}, status: {}, lokasi_valid: {}, mock: {}",
                userId, status, lokasiValid, validasiLokasi.isMockTerdeteksi());

        String pesan = buildPesanAbsen(lokasiValid, validasiLokasi, jarak, opd.getRadiusAbsen(), status);

        return AbsenResponse.builder()
                .id(absenMasuk.getId())
                .jenis("MASUK")
                .waktu(absenMasuk.getWaktuMasuk())
                .latitude(absenMasuk.getLatitude())
                .longitude(absenMasuk.getLongitude())
                .jarakDariKantor(jarak)
                .lokasiValid(lokasiValid)
                .mockLocationDetected(validasiLokasi.isMockTerdeteksi())
                .fotoAbsen(pathFoto)
                .status(status)
                .pesan(pesan)
                .build();
    }

    /**
     * Proses absen pulang
     */
    @Transactional
    public AbsenResponse absenPulang(Long userId, AbsenRequest request,
                                     MultipartFile foto, String ipAddress, String deviceInfo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AbsensiException("User tidak ditemukan"));

        LocalDate today = LocalDate.now();

        // Harus sudah absen masuk dulu
        AbsenMasuk absenMasuk = absenMasukRepository.findByUserIdAndTanggal(userId, today)
                .orElseThrow(() -> new AbsensiException("Anda belum melakukan absen masuk hari ini"));

        // Cek sudah absen pulang hari ini
        if (absenPulangRepository.existsByUserIdAndTanggal(userId, today)) {
            throw new AbsensiException("Anda sudah melakukan absen pulang hari ini");
        }

        cekIntervalAbsen(userId, "pulang");

        Opd opd = user.getOpd();

        double[] lokasiSebelumnya = redisTokenService.ambilLokasiTerakhir(userId);
        LokasiUtil.HasilValidasiLokasi validasiLokasi = lokasiUtil.validasiLokasi(
                request.getLokasi(),
                lokasiSebelumnya != null ? lokasiSebelumnya[0] : null,
                lokasiSebelumnya != null ? lokasiSebelumnya[1] : null,
                lokasiSebelumnya != null ? (long) lokasiSebelumnya[2] : null
        );

        double jarak = lokasiUtil.hitungJarak(
                request.getLokasi().getLatitude(), request.getLokasi().getLongitude(),
                opd.getLatitudeKantor(), opd.getLongitudeKantor()
        );

        boolean lokasiValid = !validasiLokasi.isMockTerdeteksi()
                && lokasiUtil.isLokasiDalamRadius(
                request.getLokasi().getLatitude(), request.getLokasi().getLongitude(),
                opd.getLatitudeKantor(), opd.getLongitudeKantor(),
                opd.getRadiusAbsen()
        );

        // Foto wajib untuk pulang
        String pathFoto = null;
        if (foto != null && !foto.isEmpty()) {
            try {
                pathFoto = fotoService.uploadFoto(foto, userId, "pulang");
            } catch (IOException e) {
                throw new AbsensiException("Gagal menyimpan foto absen pulang");
            }
        } else {
            throw new AbsensiException("Foto absen pulang wajib diisi");
        }

        // Hitung durasi kerja
        LocalDateTime sekarang = LocalDateTime.now();
        int durasiMenit = (int) ChronoUnit.MINUTES.between(absenMasuk.getWaktuMasuk(), sekarang);

        Shift shift = absenMasuk.getShift();
        StatusAbsensi status = tentukanStatusPulang(shift, durasiMenit);

        AbsenPulang absenPulang = AbsenPulang.builder()
                .user(user)
                .opd(opd)
                .shift(shift)
                .absenMasuk(absenMasuk)
                .tanggal(today)
                .waktuPulang(sekarang)
                .latitude(request.getLokasi().getLatitude())
                .longitude(request.getLokasi().getLongitude())
                .akurasiGps(request.getLokasi().getAkurasiGps())
                .jarakDariKantor(jarak)
                .fotoAbsen(pathFoto)
                .lokasiValid(lokasiValid)
                .mockLocationDetected(validasiLokasi.isMockTerdeteksi())
                .locationProvider(request.getLokasi().getLocationProvider())
                .kecepatanPerpindahan(validasiLokasi.getKecepatanPerpindahan())
                .status(status)
                .durasiKerjaMenit(durasiMenit)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .catatan(request.getCatatan())
                .build();

        absenPulangRepository.save(absenPulang);

        redisTokenService.simpanLokasiTerakhir(userId,
                request.getLokasi().getLatitude(),
                request.getLokasi().getLongitude(),
                System.currentTimeMillis());

        tandaiAbsen(userId, "pulang");

        String pesan = buildPesanAbsen(lokasiValid, validasiLokasi, jarak, opd.getRadiusAbsen(), status);

        return AbsenResponse.builder()
                .id(absenPulang.getId())
                .jenis("PULANG")
                .waktu(absenPulang.getWaktuPulang())
                .latitude(absenPulang.getLatitude())
                .longitude(absenPulang.getLongitude())
                .jarakDariKantor(jarak)
                .lokasiValid(lokasiValid)
                .mockLocationDetected(validasiLokasi.isMockTerdeteksi())
                .fotoAbsen(pathFoto)
                .status(status)
                .durasiKerjaMenit(durasiMenit)
                .pesan(pesan)
                .build();
    }

    /**
     * Cek status absen hari ini
     */
    public java.util.Map<String, Object> statusHariIni(Long userId) {
        LocalDate today = LocalDate.now();
        Optional<AbsenMasuk> masuk = absenMasukRepository.findByUserIdAndTanggal(userId, today);
        Optional<AbsenPulang> pulang = absenPulangRepository.findByUserIdAndTanggal(userId, today);

        java.util.Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("tanggal", today);
        status.put("sudahAbsenMasuk", masuk.isPresent());
        status.put("sudahAbsenPulang", pulang.isPresent());

        masuk.ifPresent(m -> {
            status.put("waktuMasuk", m.getWaktuMasuk());
            status.put("statusMasuk", m.getStatus());
        });
        pulang.ifPresent(p -> {
            status.put("waktuPulang", p.getWaktuPulang());
            status.put("statusPulang", p.getStatus());
            status.put("durasiKerjaMenit", p.getDurasiKerjaMenit());
        });

        return status;
    }

    public List<AbsenResponse> riwayatAbsenMasuk(
            Long userId,
            LocalDate dari,
            LocalDate sampai) {

        return absenMasukRepository
                .findByUserIdAndTanggalBetweenOrderByTanggalDesc(
                        userId, dari, sampai)
                .stream()
                .map(absen -> AbsenResponse.builder()
                        .id(absen.getId())
                        .jenis("MASUK")
                        .waktu(absen.getWaktuMasuk())
                        .latitude(absen.getLatitude())
                        .longitude(absen.getLongitude())
                        .jarakDariKantor(absen.getJarakDariKantor())
                        .lokasiValid(absen.getLokasiValid())
                        .mockLocationDetected(absen.getMockLocationDetected())
                        .fotoAbsen(absen.getFotoAbsen())
                        .status(absen.getStatus())
                        .build())
                .toList();
    }

    public List<AbsenResponse> riwayatAbsenPulang(
            Long userId,
            LocalDate dari,
            LocalDate sampai) {

        return absenPulangRepository
                .findByUserIdAndTanggalBetweenOrderByTanggalDesc(
                        userId, dari, sampai)
                .stream()
                .map(absen -> AbsenResponse.builder()
                        .id(absen.getId())
                        .jenis("PULANG")
                        .waktu(absen.getWaktuPulang())
                        .latitude(absen.getLatitude())
                        .longitude(absen.getLongitude())
                        .jarakDariKantor(absen.getJarakDariKantor())
                        .lokasiValid(absen.getLokasiValid())
                        .mockLocationDetected(absen.getMockLocationDetected())
                        .fotoAbsen(absen.getFotoAbsen())
                        .status(absen.getStatus())
                        .durasiKerjaMenit(absen.getDurasiKerjaMenit())
                        .build())
                .toList();
    }

    // === Helper Methods ===

    private void cekIntervalAbsen(Long userId, String jenis) {
        String key = "absen:interval:" + userId + ":" + jenis;
        Object lastAbsen = redisTokenService.get(key);
        if (lastAbsen != null) {
            throw new AbsensiException("Terlalu cepat melakukan absen. Tunggu beberapa saat.");
        }
    }

    private void tandaiAbsen(Long userId, String jenis) {
        String key = "absen:interval:" + userId + ":" + jenis;
        redisTokenService.set(key, System.currentTimeMillis(), intervalMinimalDetik * 1000L);
    }

    private Shift cariShiftAktif(Long userId, LocalDate tanggal) {
        return waktuKerjaRepository
                .findAktifByUserAndHari(userId, tanggal, tanggal.getDayOfWeek())
                .map(WaktuKerja::getShift)
                .orElse(null);
    }

    private StatusAbsensi tentukanStatusMasuk(Shift shift) {
        if (shift == null) return StatusAbsensi.HADIR;
        LocalDateTime sekarang = LocalDateTime.now();
        LocalDateTime batasLambat = LocalDate.now().atTime(shift.getJamMasuk())
                .plusMinutes(shift.getToleransiTerlambat());
        return sekarang.isAfter(batasLambat) ? StatusAbsensi.TERLAMBAT : StatusAbsensi.HADIR;
    }

    private StatusAbsensi tentukanStatusPulang(Shift shift, int durasiMenit) {
        if (shift == null) return StatusAbsensi.HADIR;
        LocalDateTime sekarang = LocalDateTime.now();
        LocalDateTime batasPulangAwal = LocalDate.now().atTime(shift.getJamPulang())
                .minusMinutes(shift.getToleransiPulangAwal());
        return sekarang.isBefore(batasPulangAwal) ? StatusAbsensi.PULANG_AWAL : StatusAbsensi.HADIR;
    }

    private String buildPesanAbsen(boolean lokasiValid, LokasiUtil.HasilValidasiLokasi validasi,
                                   double jarak, int radius, StatusAbsensi status) {
        if (validasi.isMockTerdeteksi()) {
            return "PERINGATAN: Terdeteksi kemungkinan lokasi palsu. " + validasi.getAlasan();
        }
        if (!lokasiValid) {
            return String.format("Lokasi Anda berada %.0f meter dari kantor (radius: %d meter). Absen tetap dicatat.", jarak, radius);
        }
        if (status == StatusAbsensi.TERLAMBAT) {
            return "Absen berhasil dicatat. Anda terlambat.";
        }
        if (status == StatusAbsensi.PULANG_AWAL) {
            return "Absen pulang berhasil. Anda pulang lebih awal.";
        }
        return "Absen berhasil dicatat.";
    }
}