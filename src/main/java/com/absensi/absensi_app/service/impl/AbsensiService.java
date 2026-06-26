package com.absensi.absensi_app.service.impl;

import com.absensi.absensi_app.dto.request.AbsenRequest;
import com.absensi.absensi_app.dto.response.AbsenResponse;
import com.absensi.absensi_app.dto.response.ShiftResponse;
import com.absensi.absensi_app.dto.response.UserResponse;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AbsensiService {

    private final AbsenMasukRepository absenMasukRepository;
    private final AbsenPulangRepository absenPulangRepository;
    private final WaktuKerjaRepository waktuKerjaRepository;
    private final ShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final LokasiUtil lokasiUtil;
    private final RedisTokenService redisTokenService;
    private final com.absensi.absensi_app.service.FotoService fotoService;

    @Value("${app.absensi.interval-minimal:60}")
    private int intervalMinimalDetik;

    /**
     * Proses absen masuk dengan validasi lokasi & foto
     */

    public List<ShiftResponse> daftarShiftAktif(Long userId) {
        User user = findUser(userId);
        Long opdId = user.getOpd().getId();

        return shiftRepository.findByOpdIdAndAktifTrue(opdId)
                .stream()
                .map(s -> ShiftResponse.builder()
                        .id(s.getId())
                        .nama(s.getNama())
                        .jamMasuk(s.getJamMasuk())
                        .jamPulang(s.getJamPulang())
                        .toleransiTerlambat(s.getToleransiTerlambat())
                        .toleransiPulangAwal(s.getToleransiPulangAwal())
                        .lintasHari(s.getLintasHari())
                        .aktif(s.getAktif())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public AbsenResponse absenMasuk(Long userId, AbsenRequest request,
                                    MultipartFile foto, String ipAddress, String deviceInfo) {

        if (request.getShiftId() == null) {
            throw new AbsensiException("Pilih shift terlebih dahulu sebelum absen masuk");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AbsensiException("User tidak ditemukan"));

        LocalDate today = LocalDate.now();

        // Cek sudah absen masuk hari ini
        if (absenMasukRepository.existsByUserIdAndTanggal(userId, today)) {
            throw new AbsensiException("Anda sudah melakukan absen masuk hari ini");
        }

        // Anti-spam: cek interval dari Redis
        cekIntervalAbsen(userId, "masuk");

        Shift shift = shiftRepository.findById(request.getShiftId())
                .orElseThrow(() -> new AbsensiException("Shift tidak ditemukan"));

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
        AbsenMasuk absenMasuk = cariAbsenMasukAktif(userId, user.getOpd().getId());

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
        StatusAbsensi status = tentukanStatusPulang(shift, sekarang);

        LocalDate tanggalPulang = Boolean.TRUE.equals(shift.getLintasHari())
                ? absenMasuk.getTanggal()   // Ikut tanggal masuk
                : tanggalHariIni();

        AbsenPulang absenPulang = AbsenPulang.builder()
                .user(user)
                .opd(opd)
                .shift(shift)
                .absenMasuk(absenMasuk)
                .tanggal(tanggalPulang)
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
    public Map<String, Object> statusHariIni(Long userId) {
        LocalDate today = tanggalHariIni();

        // Cari absen masuk — termasuk dari kemarin untuk shift malam lintas hari
        Optional<AbsenMasuk> masukOpt = absenMasukRepository.findByUserIdAndTanggal(userId, today);

        // Jika tidak ada hari ini, cek kemarin (kemungkinan shift malam belum pulang)
        if (masukOpt.isEmpty()) {
            masukOpt = absenMasukRepository.findByUserIdAndTanggal(userId, today.minusDays(1));
            if (masukOpt.isPresent()) {
                Shift shift = masukOpt.get().getShift();
                // Hanya relevan jika memang shift lintas hari dan belum pulang
                if (shift == null || !Boolean.TRUE.equals(shift.getLintasHari())) {
                    masukOpt = Optional.empty();
                }
            }
        }

        Optional<AbsenPulang> pulangOpt = masukOpt
                .flatMap(m -> absenPulangRepository.findByAbsenMasukId(m.getId()));

        Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("tanggal", today);
        status.put("sudahAbsenMasuk", masukOpt.isPresent());
        status.put("sudahAbsenPulang", pulangOpt.isPresent());

        masukOpt.ifPresent(m -> {
            status.put("waktuMasuk", m.getWaktuMasuk());
            status.put("statusMasuk", m.getStatus());
            status.put("shiftId", m.getShift() != null ? m.getShift().getId() : null);
            status.put("shiftNama", m.getShift() != null ? m.getShift().getNama() : null);
            status.put("shiftLintasHari", m.getShift() != null ? m.getShift().getLintasHari() : false);
        });

        pulangOpt.ifPresent(p -> {
            status.put("waktuPulang", p.getWaktuPulang());
            status.put("statusPulang", p.getStatus());
            status.put("durasiKerjaMenit", p.getDurasiKerjaMenit());
        });

        return status;
    }

    private AbsenMasuk cariAbsenMasukAktif(Long userId, Long opdId) {
        LocalDate today = tanggalHariIni();

        // Cari hari ini dulu
        Optional<AbsenMasuk> masukHariIni = absenMasukRepository.findByUserIdAndTanggal(userId, today);
        if (masukHariIni.isPresent()) return masukHariIni.get();

        // Cari kemarin — untuk shift malam lintas hari
        Optional<AbsenMasuk> masukKemarin = absenMasukRepository.findByUserIdAndTanggal(userId, today.minusDays(1));
        if (masukKemarin.isPresent()) {
            Shift shift = masukKemarin.get().getShift();
            if (shift != null && Boolean.TRUE.equals(shift.getLintasHari())) {
                // Pastikan belum ada pulang
                boolean sudahPulang = absenPulangRepository
                        .findByAbsenMasukId(masukKemarin.get().getId()).isPresent();
                if (!sudahPulang) return masukKemarin.get();
            }
        }

        throw new AbsensiException("Anda belum melakukan absen masuk. Absen masuk terlebih dahulu.");
    }

    public List<AbsenResponse> getLaporan(Long opdId, LocalDate dari, LocalDate sampai) {

        return absenMasukRepository
                .findByOpdIdAndTanggalBetween(opdId, dari, sampai)
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
                        // ⭐ USER MAPPING
                        .user(UserResponse.builder()
                                .namaLengkap(absen.getUser().getNamaLengkap())
                                .nip(absen.getUser().getNip())
                                .build())
                        .build())
                .toList();
    }

    public List<AbsenResponse> getLaporanPulang(Long opdId, LocalDate dari, LocalDate sampai) {

        return absenPulangRepository
                .findByOpdIdAndTanggalBetween(opdId, dari, sampai)
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
                        // ⭐ USER MAPPING
                        .user(UserResponse.builder()
                                .namaLengkap(absen.getUser().getNamaLengkap())
                                .nip(absen.getUser().getNip())
                                .build())
                        .build())
                .toList();
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
        LocalTime sekarang = LocalTime.now();
        LocalTime batas    = shift.batasTerlambat();
        return sekarang.isAfter(batas) ? StatusAbsensi.TERLAMBAT : StatusAbsensi.HADIR;
    }

    private StatusAbsensi tentukanStatusPulang(Shift shift, LocalDateTime sekarang) {
        LocalTime jamSekarang = sekarang.toLocalTime();
        LocalTime batas       = shift.batasPulangAwal();

        // Shift lintas hari: jika jam pulang < jam masuk, jam pulang keesokan hari
        // Pegawai yang pulang subuh (misal jam 03:00) dianggap normal
        if (Boolean.TRUE.equals(shift.getLintasHari())) {
            // Jam pulang valid = antara jam masuk sd tengah malam, atau antara 00:00 sd jam pulang
            boolean sesudahTengahMalam = jamSekarang.isBefore(shift.getJamPulang());
            if (sesudahTengahMalam) {
                // Pulang sebelum jam pulang resmi — cek batas pulang awal
                return jamSekarang.isBefore(batas) ? StatusAbsensi.PULANG_AWAL : StatusAbsensi.HADIR;
            }
            return StatusAbsensi.HADIR;
        }

        return jamSekarang.isBefore(batas) ? StatusAbsensi.PULANG_AWAL : StatusAbsensi.HADIR;
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

    private LocalDate tanggalHariIni() {
        return LocalDate.now(ZoneId.of("Asia/Jakarta"));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AbsensiException("User tidak ditemukan"));
    }
}