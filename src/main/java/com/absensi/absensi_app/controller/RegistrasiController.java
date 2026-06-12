package com.absensi.absensi_app.controller;

import com.absensi.absensi_app.dto.request.RegisterRequest;
import com.absensi.absensi_app.dto.response.ApiResponse;
import com.absensi.absensi_app.dto.response.UserDetailResponse;
import com.absensi.absensi_app.service.RedisTokenService;
import com.absensi.absensi_app.service.impl.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "Registrasi", description = "Endpoint publik untuk pendaftaran akun pegawai baru")
public class RegistrasiController {

    private final UserService userService;
    private final RedisTokenService redisTokenService;

    @PostMapping("/registrasi")
    @SecurityRequirements  // Override: endpoint ini tidak butuh auth
    @Operation(
            summary = "Registrasi akun baru (mandiri)",
            description = """
            Pegawai mendaftarkan diri sendiri. 
            
            **Catatan penting:**
            - Akun yang baru didaftarkan statusnya **TIDAK AKTIF** sampai disetujui admin
            - Admin perlu mengaktifkan akun melalui endpoint `PUT /api/v1/admin/user/{id}/aktifkan`
            - Role otomatis `ROLE_USER`, tidak bisa dipilih saat registrasi mandiri
            - NIP dan username harus unik
            """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Data registrasi pegawai",
            content = @Content(
                    schema = @Schema(implementation = RegisterRequest.class),
                    examples = @ExampleObject(value = """
                {
                  "nip": "199001012020121001",
                  "username": "budi.santoso",
                  "password": "Password123!",
                  "konfirmasiPassword": "Password123!",
                  "namaLengkap": "Budi Santoso",
                  "email": "budi.santoso@pemkot.go.id",
                  "telepon": "081234567890",
                  "opdId": 1,
                  "deviceId": "abc123device"
                }
                """)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Registrasi berhasil, menunggu aktivasi admin"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Data tidak valid / NIP atau username sudah digunakan")
    })
    public ResponseEntity<ApiResponse<UserDetailResponse>> registrasi(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        // Rate limit registrasi per IP (cegah spam)
        String ip = getClientIp(httpRequest);
        if (!redisTokenService.cekRateLimit("registrasi:" + ip, 3, 300)) {
            return ResponseEntity.status(429)
                    .body(ApiResponse.gagal("Terlalu banyak percobaan registrasi. Coba lagi 5 menit."));
        }

        UserDetailResponse result = userService.register(request);
        return ResponseEntity.ok(ApiResponse.sukses(result,
                "Registrasi berhasil! Akun Anda sedang menunggu persetujuan admin."));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isBlank()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
