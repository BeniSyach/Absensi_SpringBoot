package com.absensi.absensi_app.controller;

import com.absensi.absensi_app.dto.request.LoginRequest;
import com.absensi.absensi_app.dto.response.ApiResponse;
import com.absensi.absensi_app.dto.response.LoginResponse;
import com.absensi.absensi_app.service.RedisTokenService;
import com.absensi.absensi_app.service.impl.AuthService;
import com.absensi.absensi_app.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autentikasi", description = "Login, logout, dan refresh token JWT")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final RedisTokenService redisTokenService;

    @Value("${app.rate-limit.login-per-menit:5}")
    private int loginRateLimit;

    @PostMapping("/login")
    @SecurityRequirements  // endpoint login tidak butuh auth
    @Operation(
            summary = "Login pegawai",
            description = "Login menggunakan username dan password. Mengembalikan access token (24 jam) dan refresh token (7 hari)."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
            {"username": "admin", "password": "admin123", "deviceId": "device-abc123"}
            """))
    )
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);

        // Rate limit login per IP
        if (!redisTokenService.cekRateLimit("login:" + ip, loginRateLimit, 60)) {
            return ResponseEntity.status(429)
                    .body(ApiResponse.gagal("Terlalu banyak percobaan login. Coba lagi 1 menit."));
        }

        LoginResponse response = authService.login(request, ip);
        return ResponseEntity.ok(ApiResponse.sukses(response, "Login berhasil"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Menghapus token dari Redis dan memasukkannya ke blacklist")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Long userId = (Long) request.getAttribute("userId");
            String deviceId = (String) request.getAttribute("deviceId");
            authService.logout(token, userId, deviceId);
        }
        return ResponseEntity.ok(ApiResponse.sukses(null, "Logout berhasil"));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Refresh access token", description = "Memperpanjang sesi menggunakan refresh token. Kirim refresh token di header X-Refresh-Token.")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshTokenContoh(
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<LoginResponse>builder()
                            .success(false)
                            .error("Refresh token tidak ditemukan")
                            .build());
        }

        try {
            // ... validasi & generate access token baru (logika existing Anda) ...
             LoginResponse response = authService.refreshToken(refreshToken);
             return ResponseEntity.ok(ApiResponse.sukses(response, "Token diperbarui"));
            // throw new UnsupportedOperationException("Implementasi sesuai service Anda");

        } catch (Exception e) {
            // Refresh token expired / invalid / signature salah / user tidak ditemukan
            // → SEMUA harus 401, bukan 400 atau 500
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<LoginResponse>builder()
                            .success(false)
                            .error("Refresh token tidak valid atau sudah expired")
                            .build());
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