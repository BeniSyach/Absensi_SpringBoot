package com.absensi.absensi_app.controller;

import com.absensi.absensi_app.dto.response.ApiResponse;
import com.absensi.absensi_app.dto.response.OpdResponse;
import com.absensi.absensi_app.dto.response.ShiftResponse;
import com.absensi.absensi_app.entity.*;
import com.absensi.absensi_app.exception.AbsensiException;
import com.absensi.absensi_app.repository.*;
import com.absensi.absensi_app.service.impl.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    private final AdminService adminService;

    // === OPD Management ===

    @GetMapping("/opd")
    public ResponseEntity<ApiResponse<List<OpdResponse>>> daftarOpd() {

        return ResponseEntity.ok(
                ApiResponse.sukses(
                        adminService.getAllOpd(),
                        "Daftar OPD"
                )
        );
    }
    @PostMapping("/opd")
    public ResponseEntity<ApiResponse<Opd>> tambahOpd(@RequestBody Opd opd) {
        opd.setId(null);
        Opd saved = opdRepository.save(opd);
        return ResponseEntity.ok(ApiResponse.sukses(saved, "OPD berhasil ditambahkan"));
    }

    @PutMapping("/opd/{id}")
    public ResponseEntity<ApiResponse<Opd>> updateOpd(@PathVariable Long id, @RequestBody Opd request) {
        Opd opd = opdRepository.findById(id)
                .orElseThrow(() -> new AbsensiException("OPD tidak ditemukan"));
        opd.setNama(request.getNama());
        opd.setAlamat(request.getAlamat());
        opd.setLatitudeKantor(request.getLatitudeKantor());
        opd.setLongitudeKantor(request.getLongitudeKantor());
        opd.setRadiusAbsen(request.getRadiusAbsen());
        return ResponseEntity.ok(ApiResponse.sukses(opdRepository.save(opd), "OPD diperbarui"));
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
    public ResponseEntity<ApiResponse<Shift>> tambahShift(@RequestBody Shift shift) {
        shift.setId(null);
        return ResponseEntity.ok(ApiResponse.sukses(shiftRepository.save(shift), "Shift ditambahkan"));
    }

    // === Laporan Absensi (Admin) ===

    @GetMapping("/laporan/absen-masuk")
    public ResponseEntity<ApiResponse<List<AbsenMasuk>>> laporanAbsenMasuk(
            @RequestParam Long opdId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggal) {
        var data = absenMasukRepository.findByOpdIdAndTanggal(opdId, tanggal);
        return ResponseEntity.ok(ApiResponse.sukses(data, "Laporan absen masuk"));
    }

    @GetMapping("/laporan/absen-pulang")
    public ResponseEntity<ApiResponse<List<AbsenPulang>>> laporanAbsenPulang(
            @RequestParam Long opdId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggal) {
        var data = absenPulangRepository.findByOpdIdAndTanggal(opdId, tanggal);
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
