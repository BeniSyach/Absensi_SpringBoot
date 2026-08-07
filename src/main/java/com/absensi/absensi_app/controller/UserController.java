package com.absensi.absensi_app.controller;

import com.absensi.absensi_app.dto.request.*;
import com.absensi.absensi_app.dto.response.*;
import com.absensi.absensi_app.entity.User;
import com.absensi.absensi_app.exception.AbsensiException;
import com.absensi.absensi_app.repository.UserRepository;
import com.absensi.absensi_app.service.impl.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "User - Profil Saya", description = "Endpoint untuk user mengelola profil dan akun sendiri")

public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/profil")
    @Operation(summary = "Lihat profil saya", description = "Menampilkan data profil lengkap user yang sedang login termasuk shift hari ini")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getProfilSaya(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(ApiResponse.sukses(userService.getProfilSaya(userId), "Data profil"));
    }

    @PutMapping("/profil")
    @Operation(summary = "Update profil saya", description = "Mengubah nama lengkap, email, dan nomor telepon")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateProfil(
            @Valid @RequestBody UpdateProfilRequest req,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(ApiResponse.sukses(userService.updateProfil(userId, req), "Profil berhasil diperbarui"));
    }

    @PutMapping("/ganti-password")
    @Operation(summary = "Ganti password", description = "User mengganti password sendiri. Setelah berhasil, semua sesi aktif akan dihapus dan perlu login ulang.")
    public ResponseEntity<ApiResponse<Void>> gantiPassword(
            @Valid @RequestBody GantiPasswordRequest req,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        userService.gantiPassword(userId, req);
        return ResponseEntity.ok(ApiResponse.sukses(null, "Password berhasil diubah. Silakan login ulang."));
    }

    @PostMapping(value = "/foto-profil", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload foto profil", description = "Upload atau ganti foto profil. Format: JPG/PNG, maksimal 5MB.")
    public ResponseEntity<ApiResponse<UserDetailResponse>> uploadFotoProfil(
            @RequestPart("foto") MultipartFile foto,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(ApiResponse.sukses(userService.uploadFotoProfil(userId, foto), "Foto profil berhasil diupload"));
    }

    @DeleteMapping("/foto-profil")
    @Operation(summary = "Hapus foto profil")
    public ResponseEntity<ApiResponse<Void>> hapusFotoProfil(HttpServletRequest request) {
        Long userId = getUserId(request);
        userService.hapusFotoProfil(userId);
        return ResponseEntity.ok(ApiResponse.sukses(null, "Foto profil dihapus"));
    }

    @PutMapping("/device-id")
    @Operation(summary = "Update Device ID", description = "Dikirim otomatis oleh app Android saat login pertama kali atau ganti perangkat")
    public ResponseEntity<ApiResponse<Void>> updateDeviceId(
            @RequestParam String deviceId,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        userService.updateDeviceId(userId, deviceId);
        return ResponseEntity.ok(ApiResponse.sukses(null, "Device ID diperbarui"));
    }

    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) throw new AbsensiException("Sesi tidak valid");
        return ((Number) userId).longValue();
    }

    @GetMapping("/shift")
    @Operation(summary = "Daftar user beserta shift")
    public ResponseEntity<ApiResponse<List<UserShiftResponse>>> getUserShift(){

        List<UserShiftResponse> result =
                userRepository.findAll()
                        .stream()
                        .map(this::mapUser)
                        .toList();

        return ResponseEntity.ok(
                ApiResponse.sukses(result, "Data User")
        );
    }

    private UserShiftResponse mapUser(User user){

        return UserShiftResponse.builder()
                .id(user.getId())
                .nip(user.getNip())
                .username(user.getUsername())
                .namaLengkap(user.getNamaLengkap())
                .shiftId(user.getShift() != null ? user.getShift().getId() : null)
                .namaShift(user.getShift() != null ? user.getShift().getNama() : null)
                .build();
    }
}
