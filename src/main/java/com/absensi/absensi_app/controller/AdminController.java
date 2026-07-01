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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @Operation(summary = "Daftar shift per OPD",
            description = "Tampilkan semua shift (aktif & nonaktif) milik OPD tertentu, diurutkan jam masuk")
    public ResponseEntity<ApiResponse<List<ShiftResponse>>> daftarShift(
            @PathVariable Long opdId) {

        List<ShiftResponse> shifts = shiftRepository.findByOpdIdOrderByJamMasukAsc(opdId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.sukses(shifts, "Daftar shift"));
    }

    @PostMapping("/shift")
    @Operation(
            summary = "Buat shift baru",
            description = """
            Admin membuat shift baru untuk suatu OPD.
            
            **Field `lintasHari` dihitung otomatis** berdasarkan jam masuk dan jam pulang —
            tidak perlu diisi manual. Contoh:
            - Shift pagi  07:30–16:00 → lintasHari = false
            - Shift siang 12:00–20:00 → lintasHari = false  
            - Shift malam 20:00–04:00 → lintasHari = **true** (04:00 < 20:00)
            """
    )
    public ResponseEntity<ApiResponse<ShiftResponse>> buatShift(
            @Valid @RequestBody ShiftRequest request) {

        Opd opd = opdRepository.findById(request.getOpdId())
                .orElseThrow(() -> new AbsensiException("OPD tidak ditemukan"));

        Shift shift = Shift.builder()
                .nama(request.getNama())
                .jamMasuk(request.getJamMasuk())
                .jamPulang(request.getJamPulang())
                .toleransiTerlambat(request.getToleransiTerlambat())
                .toleransiPulangAwal(request.getToleransiPulangAwal())
                .opd(opd)
                .aktif(true)
                .build();

        // lintasHari dihitung otomatis oleh @PrePersist di entity
        Shift saved = shiftRepository.save(shift);

        return ResponseEntity.ok(ApiResponse.sukses(toResponse(saved),
                "Shift berhasil dibuat. lintasHari: " + saved.getLintasHari()));
    }

    @PutMapping("/shift/{id}")
    @Operation(summary = "Update shift",
            description = "Ubah nama, jam, atau toleransi. `lintasHari` otomatis dihitung ulang.")
    public ResponseEntity<ApiResponse<ShiftResponse>> updateShift(
            @PathVariable Long id,
            @Valid @RequestBody ShiftRequest request) {

        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new AbsensiException("Shift tidak ditemukan"));

        Opd opd = opdRepository.findById(request.getOpdId())
                .orElseThrow(() -> new AbsensiException("OPD tidak ditemukan"));

        shift.setNama(request.getNama());
        shift.setJamMasuk(request.getJamMasuk());
        shift.setJamPulang(request.getJamPulang());
        shift.setToleransiTerlambat(request.getToleransiTerlambat());
        shift.setToleransiPulangAwal(request.getToleransiPulangAwal());
        shift.setOpd(opd);
        // lintasHari dihitung ulang oleh @PreUpdate

        return ResponseEntity.ok(ApiResponse.sukses(toResponse(shiftRepository.save(shift)),
                "Shift diperbarui"));
    }

    @DeleteMapping("/shift/{id}/nonaktifkan")
    @Operation(summary = "Nonaktifkan shift",
            description = "Shift tidak akan muncul di dropdown pegawai, tapi data historis tetap ada")
    public ResponseEntity<ApiResponse<Void>> nonaktifkanShift(@PathVariable Long id) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new AbsensiException("Shift tidak ditemukan"));
        shift.setAktif(false);
        shiftRepository.save(shift);
        return ResponseEntity.ok(ApiResponse.sukses(null, "Shift dinonaktifkan"));
    }
    @PutMapping("/shift/{id}/aktifkan")
    @Operation(summary = "Aktifkan kembali shift")
    public ResponseEntity<ApiResponse<Void>> aktifkanShift(@PathVariable Long id) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new AbsensiException("Shift tidak ditemukan"));
        shift.setAktif(true);
        shiftRepository.save(shift);
        return ResponseEntity.ok(ApiResponse.sukses(null, "Shift diaktifkan"));
    }

    private ShiftResponse toResponse(Shift s) {
        return ShiftResponse.builder()
                .id(s.getId())
                .nama(s.getNama())
                .jamMasuk(s.getJamMasuk())
                .jamPulang(s.getJamPulang())
                .toleransiTerlambat(s.getToleransiTerlambat())
                .toleransiPulangAwal(s.getToleransiPulangAwal())
                .lintasHari(s.getLintasHari())
                .aktif(s.getAktif())
                .build();
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
