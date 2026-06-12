package com.absensi.absensi_app.controller;

import com.absensi.absensi_app.dto.request.AbsenRequest;
import com.absensi.absensi_app.dto.response.AbsenResponse;
import com.absensi.absensi_app.dto.response.ApiResponse;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/absensi")
@RequiredArgsConstructor
@Tag(name = "Absensi", description = "Endpoint absen masuk dan pulang, status, dan riwayat")
public class AbsensiController {

    private final AbsensiService absensiService;
    private final RedisTokenService redisTokenService;
    private final ObjectMapper objectMapper;

    @Value("${app.rate-limit.absen-per-menit:10}")
    private int absenRateLimit;

    /**
     * Absen masuk - multipart: foto + data JSON
     */
    @PostMapping(value = "/masuk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Absen masuk",
            description = """
            Melakukan absen masuk dengan upload foto dan data GPS.
            
            **Format request:** `multipart/form-data` dengan dua part:
            - `foto` → file gambar (JPG/PNG, maks 5MB)  
            - `data` → JSON string: `{"lokasi":{"latitude":-3.59,"longitude":98.67,"akurasiGps":12.5,"locationProvider":"fused","isMockLocation":false}}`
            
            **Deteksi lokasi palsu:** Server akan otomatis mendeteksi mock location berdasarkan akurasi GPS, provider, dan kecepatan perpindahan.
            """
    )
    public ResponseEntity<ApiResponse<AbsenResponse>> absenMasuk(
            @RequestPart("foto") MultipartFile foto,
            @RequestPart("data") String dataJson,
            HttpServletRequest request) throws Exception {

        Long userId = getUserId(request);
        cekRateLimit(userId, "masuk");

        AbsenRequest absenRequest = objectMapper.readValue(dataJson, AbsenRequest.class);
        String ip = getClientIp(request);
        String deviceInfo = request.getHeader("X-Device-Info");

        AbsenResponse response = absensiService.absenMasuk(userId, absenRequest, foto, ip, deviceInfo);
        return ResponseEntity.ok(ApiResponse.sukses(response, "Absen masuk berhasil"));
    }

    /**
     * Absen pulang - multipart: foto + data JSON
     */
    @PostMapping(value = "/pulang", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Absen pulang", description = "Wajib sudah absen masuk terlebih dahulu di hari yang sama. Format sama dengan absen masuk.")
    public ResponseEntity<ApiResponse<AbsenResponse>> absenPulang(
            @RequestPart("foto") MultipartFile foto,
            @RequestPart("data") String dataJson,
            HttpServletRequest request) throws Exception {

        Long userId = getUserId(request);
        cekRateLimit(userId, "pulang");

        AbsenRequest absenRequest = objectMapper.readValue(dataJson, AbsenRequest.class);
        String ip = getClientIp(request);
        String deviceInfo = request.getHeader("X-Device-Info");

        AbsenResponse response = absensiService.absenPulang(userId, absenRequest, foto, ip, deviceInfo);
        return ResponseEntity.ok(ApiResponse.sukses(response, "Absen pulang berhasil"));
    }

    /**
     * Status absen hari ini
     */
    @GetMapping("/status/hari-ini")
    @Operation(summary = "Status absen hari ini", description = "Cek apakah sudah absen masuk dan/atau pulang hari ini, beserta waktu dan statusnya")
    public ResponseEntity<ApiResponse<Map<String, Object>>> statusHariIni(HttpServletRequest request) {
        Long userId = getUserId(request);
        Map<String, Object> status = absensiService.statusHariIni(userId);
        return ResponseEntity.ok(ApiResponse.sukses(status, "Status absen hari ini"));
    }

    /**
     * Riwayat absen masuk
     */
    @GetMapping("/riwayat/masuk")
    public ResponseEntity<ApiResponse<?>> riwayatMasuk(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dari,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sampai) {

        Long userId = getUserId(request);
        var data = absensiService.riwayatAbsenMasuk(userId, dari, sampai);
        return ResponseEntity.ok(ApiResponse.sukses(data, "Riwayat absen masuk"));
    }

    /**
     * Riwayat absen pulang
     */
    @GetMapping("/riwayat/pulang")
    public ResponseEntity<ApiResponse<?>> riwayatPulang(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dari,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sampai) {

        Long userId = getUserId(request);
        var data = absensiService.riwayatAbsenPulang(userId, dari, sampai);
        return ResponseEntity.ok(ApiResponse.sukses(data, "Riwayat absen pulang"));
    }

    // === Helper ===

    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) throw new com.absensi.absensi_app.exception.AbsensiException("Sesi tidak valid");
        return ((Number) userId).longValue();
    }

    private void cekRateLimit(Long userId, String aksi) {
        if (!redisTokenService.cekRateLimit("absen:" + aksi + ":" + userId, absenRateLimit, 60)) {
            throw new com.absensi.absensi_app.exception.AbsensiException("Terlalu banyak permintaan. Coba lagi sebentar.");
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