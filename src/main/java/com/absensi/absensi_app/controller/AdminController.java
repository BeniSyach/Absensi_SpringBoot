package com.absensi.absensi_app.controller;

import com.absensi.absensi_app.dto.request.ShiftRequest;
import com.absensi.absensi_app.dto.response.AbsenResponse;
import com.absensi.absensi_app.dto.response.ApiResponse;
import com.absensi.absensi_app.dto.response.OpdResponse;
import com.absensi.absensi_app.dto.response.ShiftResponse;
import com.absensi.absensi_app.entity.*;
import com.absensi.absensi_app.exception.AbsensiException;
import com.absensi.absensi_app.repository.*;
import com.absensi.absensi_app.service.impl.AbsensiService;
import com.absensi.absensi_app.service.impl.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OpdRepository opdRepository;
    private final ShiftRepository shiftRepository;
    private final AbsenMasukRepository absenMasukRepository;
    private final AbsenPulangRepository absenPulangRepository;
    private final AbsensiService absensiService;
    private final UserRepository userRepository;
    private final WaktuKerjaRepository waktuKerjaRepository;

    private final AdminService adminService;

    // === OPD Management ===

    @GetMapping("/opd")
    public ResponseEntity<ApiResponse<List<OpdResponse>>> daftarOpd() {

        return ResponseEntity.ok(
                ApiResponse.sukses(
                        adminService.getAllOpd(),
                        "Daftar Titik Lokasi"
                )
        );
    }
    @PostMapping("/opd")
    public ResponseEntity<ApiResponse<Opd>> tambahOpd(@RequestBody Opd opd) {
        opd.setId(null);
        Opd saved = opdRepository.save(opd);
        return ResponseEntity.ok(ApiResponse.sukses(saved, "Titik Lokasi berhasil ditambahkan"));
    }

    @PutMapping("/opd/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateOpd(
            @PathVariable Long id,
            @RequestBody Opd request) {

        Opd opd = opdRepository.findById(id)
                .orElseThrow(() -> new AbsensiException("Titik Lokasi tidak ditemukan"));

        opd.setNama(request.getNama());
        opd.setAlamat(request.getAlamat());
        opd.setLatitudeKantor(request.getLatitudeKantor());
        opd.setLongitudeKantor(request.getLongitudeKantor());
        opd.setRadiusAbsen(request.getRadiusAbsen());

        opd = opdRepository.save(opd);

        Map<String, Object> response = new HashMap<>();
        response.put("id", opd.getId());
        response.put("kode", opd.getKode());
        response.put("nama", opd.getNama());
        response.put("alamat", opd.getAlamat());
        response.put("latitudeKantor", opd.getLatitudeKantor());
        response.put("longitudeKantor", opd.getLongitudeKantor());
        response.put("radiusAbsen", opd.getRadiusAbsen());
        response.put("aktif", opd.getAktif());

        return ResponseEntity.ok(
                ApiResponse.sukses(response, "Titik Lokasi diperbarui")
        );
    }

    @DeleteMapping("/opd/{id}")
    public ResponseEntity<ApiResponse<Void>> hapusOpd(@PathVariable Long id) {

        Opd opd = opdRepository.findById(id)
                .orElseThrow(() -> new AbsensiException("Titik Lokasi tidak ditemukan"));

        opd.setAktif(false);

        opdRepository.save(opd);

        return ResponseEntity.ok(
                ApiResponse.sukses(null, "Titik Lokasi berhasil dihapus")
        );
    }

    // === Shift Management ===

    @GetMapping("/shift/{opdId}")
    public ResponseEntity<ApiResponse<List<ShiftResponse>>> daftarShift(@PathVariable Long opdId) {

        return ResponseEntity.ok(
                ApiResponse.sukses(
                        adminService.getShiftByOpd(opdId),
                        "Daftar shift"
                )
        );
    }

    @PostMapping("/shift")
    public ResponseEntity<?> tambahShift(
            @RequestBody ShiftRequest request) {

        Opd opd = opdRepository.findById(request.getOpdId())
                .orElseThrow();

        Shift shift = Shift.builder()
                .nama(request.getNama())
                .jamMasuk(request.getJamMasuk())
                .jamPulang(request.getJamPulang())
                .toleransiTerlambat(request.getToleransiTerlambat())
                .toleransiPulangAwal(request.getToleransiPulangAwal())
                .aktif(request.getAktif())
                .opd(opd)
                .build();

        shift = shiftRepository.save(shift);

        if (request.getUserId() != null) {

            User user = userRepository.findById(request.getUserId())
                    .orElseThrow();

            WaktuKerja waktuKerja = WaktuKerja.builder()
                    .shift(shift)
                    .user(user)
                    .hariKerja(request.getHariKerja())
                    .tanggalMulai(request.getTanggalMulai())
                    .tanggalSelesai(request.getTanggalSelesai())
                    .aktif(true)
                    .build();

            waktuKerjaRepository.save(waktuKerja);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", shift.getId());
        response.put("nama", shift.getNama());
        response.put("jamMasuk", shift.getJamMasuk());
        response.put("jamPulang", shift.getJamPulang());
        response.put("toleransiTerlambat", shift.getToleransiTerlambat());
        response.put("toleransiPulangAwal", shift.getToleransiPulangAwal());
        response.put("aktif", shift.getAktif());
        response.put("opdId", shift.getOpd().getId());

        return ResponseEntity.ok(
                ApiResponse.sukses(response, "Shift berhasil dibuat")
        );
    }

    @PutMapping("/shift/{id}")
    public ResponseEntity<?> updateShift(
            @PathVariable Long id,
            @RequestBody ShiftRequest request) {

        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new AbsensiException("Shift tidak ditemukan"));

        Opd opd = opdRepository.findById(request.getOpdId())
                .orElseThrow(() -> new AbsensiException("OPD tidak ditemukan"));

        shift.setNama(request.getNama());
        shift.setJamMasuk(request.getJamMasuk());
        shift.setJamPulang(request.getJamPulang());
        shift.setToleransiTerlambat(request.getToleransiTerlambat());
        shift.setToleransiPulangAwal(request.getToleransiPulangAwal());
        shift.setAktif(request.getAktif());
        shift.setOpd(opd);

        Shift savedShift = shiftRepository.save(shift);

        // Update waktu kerja jika dikirim userId
        if (request.getUserId() != null) {

            WaktuKerja waktuKerja = waktuKerjaRepository
                    .findByShiftIdAndUserId(savedShift.getId(), request.getUserId())
                    .orElse(null);

            if (waktuKerja == null) {

                User user = userRepository.findById(request.getUserId())
                        .orElseThrow(() -> new AbsensiException("User tidak ditemukan"));

                waktuKerja = new WaktuKerja();
                waktuKerja.setShift(savedShift);
                waktuKerja.setUser(user);
            }

            waktuKerja.setHariKerja(request.getHariKerja());
            waktuKerja.setTanggalMulai(request.getTanggalMulai());
            waktuKerja.setTanggalSelesai(request.getTanggalSelesai());
            waktuKerja.setAktif(true);

            waktuKerjaRepository.save(waktuKerja);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", savedShift.getId());
        response.put("nama", savedShift.getNama());
        response.put("jamMasuk", savedShift.getJamMasuk());
        response.put("jamPulang", savedShift.getJamPulang());
        response.put("toleransiTerlambat", savedShift.getToleransiTerlambat());
        response.put("toleransiPulangAwal", savedShift.getToleransiPulangAwal());
        response.put("aktif", savedShift.getAktif());
        response.put("opdId", savedShift.getOpd().getId());

        return ResponseEntity.ok(
                ApiResponse.sukses(response, "Shift berhasil diperbarui")
        );
    }

    @DeleteMapping("/shift/{id}")
    public ResponseEntity<ApiResponse<?>> hapusShift(@PathVariable Long id) {

        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new AbsensiException("Shift tidak ditemukan"));

        shift.setAktif(false);

        shiftRepository.save(shift);

        return ResponseEntity.ok(
                ApiResponse.sukses(null, "Shift berhasil dihapus")
        );
    }

    // === Laporan Absensi (Admin) ===

    @GetMapping("/laporan/absen-masuk")
    public ResponseEntity<ApiResponse<List<AbsenResponse>>> laporanAbsenMasuk(
            @RequestParam Long opdId,
            @RequestParam LocalDate tanggalAwal,
            @RequestParam LocalDate tanggalSampai
    ) {
        var data = absensiService.getLaporan(opdId, tanggalAwal, tanggalSampai);
        return ResponseEntity.ok(ApiResponse.sukses(data, "Laporan absen masuk"));
    }

    @GetMapping("/laporan/absen-pulang")
    public ResponseEntity<ApiResponse<List<AbsenResponse>>> laporanAbsenPulang(
            @RequestParam Long opdId,
            @RequestParam LocalDate tanggalAwal,
            @RequestParam LocalDate tanggalSampai
    ) {
        var data = absensiService.getLaporanPulang(opdId, tanggalAwal, tanggalSampai);
        return ResponseEntity.ok(ApiResponse.sukses(data, "Laporan absen pulang"));
    }

    @GetMapping("/laporan/rekap-user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rekapUser(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dari,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sampai) {

        long totalMasuk = absenMasukRepository.countByUserIdAndPeriode(userId, dari, sampai);

        return ResponseEntity.ok(ApiResponse.sukses(
                Map.of("totalHadir", totalMasuk, "periode", dari + " s/d " + sampai),
                "Rekap absensi user"));
    }
}
