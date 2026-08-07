package com.absensi.absensi_app.controller;

import com.absensi.absensi_app.dto.request.ShiftRequest;
import com.absensi.absensi_app.dto.request.WaktuKerjaRequest;
import com.absensi.absensi_app.dto.response.*;
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

        opd.setKode(request.getKode());
        opd.setNama(request.getNama());
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
            """
    )
    public ResponseEntity<ApiResponse<ShiftResponse>> buatShift(
            @Valid @RequestBody ShiftRequest request) {

        Opd opd = opdRepository.findById(request.getOpdId())
                .orElseThrow(() -> new AbsensiException("OPD tidak ditemukan"));


        Shift shift = Shift.builder()
                .nama(request.getNama())
                .opd(opd)
                .aktif(true)
                .build();


        Shift savedShift = shiftRepository.save(shift);


        return ResponseEntity.ok(
                ApiResponse.sukses(
                        toResponse(savedShift),
                        "Shift berhasil dibuat"
                )
        );
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
        shift.setOpd(opd);


        Shift updated = shiftRepository.save(shift);


        return ResponseEntity.ok(
                ApiResponse.sukses(
                        toResponse(updated),
                        "Shift diperbarui"
                )
        );
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

        List<WaktuKerjaResponse> waktuKerja =
                s.getWaktuKerja() == null
                        ? List.of()
                        : s.getWaktuKerja()
                        .stream()
                        .map(this::toWaktuKerjaResponse)
                        .collect(Collectors.toList());


        return ShiftResponse.builder()
                .id(s.getId())
                .nama(s.getNama())
                .aktif(s.getAktif())
                .opdId(s.getOpd().getId())
                .namaOpd(s.getOpd().getNama())
                .waktuKerja(waktuKerja)
                .build();
    }

    private WaktuKerjaResponse toWaktuKerjaResponse(WaktuKerja w) {

        return WaktuKerjaResponse.builder()
                .id(w.getId())
                .hari(w.getHari())
                .jamMasuk(w.getJamMasuk())
                .jamPulang(w.getJamPulang())
                .toleransiTerlambat(w.getToleransiTerlambat())
                .toleransiPulangAwal(w.getToleransiPulangAwal())
                .lintasHari(w.getLintasHari())
                .aktif(w.getAktif())
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

    private WaktuKerjaResponse toResponse(WaktuKerja w) {

        return WaktuKerjaResponse.builder()
                .id(w.getId())
                .hari(w.getHari())
                .jamMasuk(w.getJamMasuk())
                .jamPulang(w.getJamPulang())
                .toleransiTerlambat(w.getToleransiTerlambat())
                .toleransiPulangAwal(w.getToleransiPulangAwal())
                .lintasHari(w.getLintasHari())
                .aktif(w.getAktif())
                .build();
    }

    @PostMapping("/shift/{shiftId}/waktu-kerja")
    public ResponseEntity<ApiResponse<WaktuKerjaResponse>> tambahWaktuKerja(
            @PathVariable Long shiftId,
            @Valid @RequestBody WaktuKerjaRequest request
    ) {

        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new AbsensiException("Shift tidak ditemukan"));


        boolean lintasHari = request.getJamPulang()
                .isBefore(request.getJamMasuk());


        WaktuKerja waktuKerja = WaktuKerja.builder()
                .shift(shift)
                .hari(request.getHari())
                .jamMasuk(request.getJamMasuk())
                .jamPulang(request.getJamPulang())
                .toleransiTerlambat(request.getToleransiTerlambat())
                .toleransiPulangAwal(request.getToleransiPulangAwal())
                .lintasHari(lintasHari)
                .aktif(true)
                .build();


        WaktuKerja saved = waktuKerjaRepository.save(waktuKerja);


        return ResponseEntity.ok(
                ApiResponse.sukses(
                        toResponse(saved),
                        "Waktu kerja berhasil ditambahkan"
                )
        );
    }

    // ============================================
// EDIT (UPDATE) WAKTU KERJA
// ============================================
    @PutMapping("/shift/{shiftId}/waktu-kerja/{id}")
    public ResponseEntity<ApiResponse<WaktuKerjaResponse>> editWaktuKerja(
            @PathVariable Long shiftId,
            @PathVariable Long id,
            @Valid @RequestBody WaktuKerjaRequest request
    ) {

        // 1. Validasi shift masih ada
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new AbsensiException("Shift tidak ditemukan"));

        // 2. Cari waktuKerja dan pastikan milik shift yang sama
        WaktuKerja waktuKerja = waktuKerjaRepository.findById(id)
                .orElseThrow(() -> new AbsensiException("Waktu kerja tidak ditemukan"));

        if (!waktuKerja.getShift().getId().equals(shiftId)) {
            throw new AbsensiException("Waktu kerja tidak sesuai dengan shift");
        }

        // 3. Deteksi otomatis lintasHari (jika jamPulang < jamMasuk = lintas hari)
        boolean lintasHari = request.getJamPulang().isBefore(request.getJamMasuk());

        // 4. Update field
        waktuKerja.setHari(request.getHari());
        waktuKerja.setJamMasuk(request.getJamMasuk());
        waktuKerja.setJamPulang(request.getJamPulang());
        waktuKerja.setToleransiTerlambat(request.getToleransiTerlambat());
        waktuKerja.setToleransiPulangAwal(request.getToleransiPulangAwal());
        waktuKerja.setLintasHari(lintasHari);

        // 5. Simpan & return
        WaktuKerja updated = waktuKerjaRepository.save(waktuKerja);

        return ResponseEntity.ok(
                ApiResponse.sukses(
                        toResponse(updated),
                        "Waktu kerja berhasil diperbarui"
                )
        );
    }

    // ============================================
// DELETE WAKTU KERJA
// ============================================
    @DeleteMapping("/shift/{shiftId}/waktu-kerja/{id}")
    public ResponseEntity<ApiResponse<Void>> hapusWaktuKerja(
            @PathVariable Long shiftId,
            @PathVariable Long id
    ) {

        // 1. Validasi shift masih ada
        shiftRepository.findById(shiftId)
                .orElseThrow(() -> new AbsensiException("Shift tidak ditemukan"));

        // 2. Cari waktuKerja dan pastikan milik shift yang sama
        WaktuKerja waktuKerja = waktuKerjaRepository.findById(id)
                .orElseThrow(() -> new AbsensiException("Waktu kerja tidak ditemukan"));

        if (!waktuKerja.getShift().getId().equals(shiftId)) {
            throw new AbsensiException("Waktu kerja tidak sesuai dengan shift");
        }

        // 3. Hapus
        waktuKerjaRepository.delete(waktuKerja);

        return ResponseEntity.ok(
                ApiResponse.sukses(null, "Waktu kerja berhasil dihapus")
        );
    }

    // ============================================
// (BONUS) LIST WAKTU KERJA PER SHIFT
// ============================================
    @GetMapping("/shift/{shiftId}/waktu-kerja")
    public ResponseEntity<ApiResponse<List<WaktuKerjaResponse>>> listWaktuKerja(
            @PathVariable Long shiftId
    ) {

        // 1. Validasi shift masih ada
        shiftRepository.findById(shiftId)
                .orElseThrow(() -> new AbsensiException("Shift tidak ditemukan"));

        // 2. Ambil semua waktu kerja milik shift tersebut
        List<WaktuKerja> list = waktuKerjaRepository.findByShiftId(shiftId);

        // 3. Map ke response
        List<WaktuKerjaResponse> responseList = list.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(
                ApiResponse.sukses(responseList, "Daftar waktu kerja")
        );
    }
}
