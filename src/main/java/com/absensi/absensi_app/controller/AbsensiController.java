package com.absensi.absensi_app.controller;

import com.absensi.absensi_app.dto.request.AbsenRequest;
import com.absensi.absensi_app.dto.response.AbsenResponse;
import com.absensi.absensi_app.dto.response.ApiResponse;
import com.absensi.absensi_app.dto.response.ShiftResponse;
import com.absensi.absensi_app.exception.AbsensiException;
import com.absensi.absensi_app.service.RedisTokenService;
import com.absensi.absensi_app.service.impl.AbsensiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/absensi")
@RequiredArgsConstructor
@Tag(name = "Absensi", description = "Absen masuk/pulang dengan pemilihan shift mandiri oleh pegawai")
public class AbsensiController {

    private final AbsensiService  absensiService;
    private final RedisTokenService redisTokenService;
    private final ObjectMapper    objectMapper;

    @Value("${app.rate-limit.absen-per-menit:3}")
    private int absenRateLimit;

    // ─────────────────────────────────────────────────────────────
    // GET: daftar shift yang bisa dipilih pegawai
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/shift/available")
    @Operation(
            summary = "Daftar shift yang tersedia",
            description = """
            Mengembalikan daftar shift aktif milik OPD user yang sedang login.
            Digunakan Android untuk membangun dropdown pilih shift sebelum absen masuk.
            
            **Tidak perlu parameter** — server otomatis ambil OPD dari token JWT.
            """
    )
    public ResponseEntity<ApiResponse<List<ShiftResponse>>> daftarShiftAvailable(
            HttpServletRequest request) {

        Long userId = getUserId(request);
        List<ShiftResponse> shifts = absensiService.daftarShiftAktif(userId);
        return ResponseEntity.ok(ApiResponse.sukses(shifts, "Daftar shift tersedia"));
    }

    // ─────────────────────────────────────────────────────────────
    // POST: absen masuk — user WAJIB kirim shiftId
    // ─────────────────────────────────────────────────────────────

    @PostMapping(value = "/masuk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Absen masuk",
            description = """
            Melakukan absen masuk dengan memilih shift terlebih dahulu.
            
            **Format request:** `multipart/form-data`
            - `foto` → selfie JPG/PNG, maks 5MB
            - `data` → JSON:
            ```json
            {
              "shiftId": 1,
              "lokasi": {
                "latitude": -3.59, "longitude": 98.67,
                "akurasiGps": 12.5, "locationProvider": "fused",
                "isMockLocation": false
              },
              "catatan": "opsional"
            }
            ```
            """
    )
    public ResponseEntity<ApiResponse<AbsenResponse>> absenMasuk(
            @RequestPart("foto") MultipartFile foto,
            @RequestPart("data") String dataJson,
            HttpServletRequest request) throws Exception {

        Long userId = getUserId(request);
        cekRateLimit(userId, "masuk");

        AbsenRequest absenRequest = objectMapper.readValue(dataJson, AbsenRequest.class);

        // Validasi wajib: shiftId harus ada
        if (absenRequest.getShiftId() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.gagal("shiftId wajib diisi. Pilih shift terlebih dahulu."));
        }

        String ip         = getClientIp(request);
        String deviceInfo = request.getHeader("X-Device-Info");

        AbsenResponse response = absensiService.absenMasuk(userId, absenRequest, foto, ip, deviceInfo);
        return ResponseEntity.ok(ApiResponse.sukses(response, "Absen masuk berhasil"));
    }

    // ─────────────────────────────────────────────────────────────
    // POST: absen pulang — shiftId TIDAK perlu, diambil dari absen masuk
    // ─────────────────────────────────────────────────────────────

    @PostMapping(value = "/pulang", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Absen pulang",
            description = """
            Absen pulang — **tidak perlu memilih shift lagi**, server otomatis menggunakan
            shift yang dipilih saat absen masuk hari ini.
            
            Mendukung **shift malam lintas hari**: jika absen masuk kemarin malam
            dan belum pulang, absen pulang tetap bisa dilakukan hari ini.
            """
    )
    public ResponseEntity<ApiResponse<AbsenResponse>> absenPulang(
            @RequestPart("foto") MultipartFile foto,
            @RequestPart("data") String dataJson,
            HttpServletRequest request) throws Exception {

        Long userId = getUserId(request);
        cekRateLimit(userId, "pulang");

        AbsenRequest absenRequest = objectMapper.readValue(dataJson, AbsenRequest.class);
        String ip         = getClientIp(request);
        String deviceInfo = request.getHeader("X-Device-Info");

        AbsenResponse response = absensiService.absenPulang(userId, absenRequest, foto, ip, deviceInfo);
        return ResponseEntity.ok(ApiResponse.sukses(response, "Absen pulang berhasil"));
    }

    // ─────────────────────────────────────────────────────────────
    // GET: status hari ini
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/status/hari-ini")
    @Operation(
            summary = "Status absen hari ini",
            description = """
            Cek status absen user hari ini. Termasuk info shift yang dipilih,
            dan menangani shift malam lintas hari.
            """
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> statusHariIni(
            HttpServletRequest request) {

        Long userId = getUserId(request);
        Map<String, Object> status = absensiService.statusHariIni(userId);
        return ResponseEntity.ok(ApiResponse.sukses(status, "Status absen hari ini"));
    }

    // ─────────────────────────────────────────────────────────────
    // GET: riwayat
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/riwayat/masuk")
    @Operation(summary = "Riwayat absen masuk")
    public ResponseEntity<ApiResponse<?>> riwayatMasuk(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dari,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sampai) {

        Long userId = getUserId(request);
        return ResponseEntity.ok(ApiResponse.sukses(
                absensiService.riwayatAbsenMasuk(userId, dari, sampai),
                "Riwayat absen masuk"));
    }

    @GetMapping("/riwayat/pulang")
    @Operation(summary = "Riwayat absen pulang")
    public ResponseEntity<ApiResponse<?>> riwayatPulang(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dari,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sampai) {

        Long userId = getUserId(request);
        return ResponseEntity.ok(ApiResponse.sukses(
                absensiService.riwayatAbsenPulang(userId, dari, sampai),
                "Riwayat absen pulang"));
    }

    // ─────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────

    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) throw new AbsensiException("Sesi tidak valid");
        return ((Number) userId).longValue();
    }

    private void cekRateLimit(Long userId, String aksi) {
        if (!redisTokenService.cekRateLimit("absen:" + aksi + ":" + userId, absenRateLimit, 60)) {
            throw new AbsensiException("Terlalu banyak permintaan. Coba lagi sebentar.");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isBlank()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
