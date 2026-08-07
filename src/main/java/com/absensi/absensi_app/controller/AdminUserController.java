package com.absensi.absensi_app.controller;

import com.absensi.absensi_app.dto.request.*;
import com.absensi.absensi_app.dto.response.*;
import com.absensi.absensi_app.enums.Role;
import com.absensi.absensi_app.service.impl.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/user")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Manajemen User", description = "Endpoint khusus admin untuk mengelola seluruh data pegawai")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(
            summary = "Daftar semua user",
            description = "Mendapatkan daftar user dengan pagination, bisa filter berdasarkan keyword, OPD, dan status aktif"
    )
    public ResponseEntity<ApiResponse<PageResponse<UserDetailResponse>>> daftarUser(
            @Parameter(description = "Cari berdasarkan nama, username, atau NIP")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter berdasarkan ID OPD")
            @RequestParam(required = false) Long opdId,
            @Parameter(description = "Filter berdasarkan status: true=aktif, false=nonaktif")
            @RequestParam(required = false) Boolean aktif,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<UserDetailResponse> result = userService.daftarUser(keyword, opdId, aktif, page, size);
        return ResponseEntity.ok(ApiResponse.sukses(result, "Daftar user"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail user", description = "Mendapatkan detail lengkap satu user termasuk shift aktif")
    public ResponseEntity<ApiResponse<UserDetailResponse>> detailUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.sukses(userService.getDetailUser(id), "Detail user"));
    }

    @PostMapping
    @Operation(
            summary = "Tambah user baru (oleh admin)",
            description = "Admin membuat akun user baru. User langsung aktif tanpa menunggu persetujuan. Role bisa ditentukan."
    )
    public ResponseEntity<ApiResponse<UserDetailResponse>> tambahUser(
            @Valid @RequestBody RegisterRequest request,
            @Parameter(description = "Role user: ROLE_USER, ROLE_ADMIN, ROLE_PIMPINAN")
            @RequestParam(defaultValue = "ROLE_USER") String role) {

        Role roleEnum = Role.valueOf(role);
        UserDetailResponse result = userService.adminTambahUser(request, roleEnum);
        return ResponseEntity.ok(ApiResponse.sukses(result, "User berhasil dibuat"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update data user", description = "Admin mengubah data user: NIP, nama, OPD, role, status aktif")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.sukses(userService.adminUpdateUser(id, request), "User berhasil diperbarui"));
    }

    @PutMapping("/{id}/reset-password")
    @Operation(
            summary = "Reset password user",
            description = "Admin mereset password user. User akan dipaksa logout dan harus login ulang dengan password baru."
    )
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request) {
        userService.adminResetPassword(id, request);
        return ResponseEntity.ok(ApiResponse.sukses(null, "Password user berhasil direset"));
    }

    @PutMapping("/{id}/aktifkan")
    @Operation(summary = "Aktifkan user", description = "Mengaktifkan user yang sebelumnya nonaktif atau baru registrasi mandiri")
    public ResponseEntity<ApiResponse<Void>> aktifkanUser(@PathVariable Long id) {
        userService.adminAktivasiUser(id, true);
        return ResponseEntity.ok(ApiResponse.sukses(null, "User berhasil diaktifkan"));
    }

    @PutMapping("/{id}/nonaktifkan")
    @Operation(summary = "Nonaktifkan user", description = "Menonaktifkan user. User tidak bisa login dan sesi aktif dihapus.")
    public ResponseEntity<ApiResponse<Void>> nonaktifkanUser(@PathVariable Long id) {
        userService.adminAktivasiUser(id, false);
        return ResponseEntity.ok(ApiResponse.sukses(null, "User berhasil dinonaktifkan"));
    }

    @DeleteMapping("/{id}/device-id")
    @Operation(
            summary = "Hapus Device ID user",
            description = "Menghapus binding device ID dan paksa logout. Berguna jika pegawai ganti HP atau HP hilang."
    )
    public ResponseEntity<ApiResponse<Void>> hapusDeviceId(@PathVariable Long id) {
        userService.adminHapusDeviceId(id);
        return ResponseEntity.ok(ApiResponse.sukses(null, "Device ID dihapus, user telah dilogout"));
    }

    @PostMapping("/{id}/paksa-logout")
    @Operation(summary = "Paksa logout user", description = "Admin memaksa logout user tertentu. Token aktif dihapus dari Redis.")
    public ResponseEntity<ApiResponse<Void>> paksaLogout(@PathVariable Long id) {
        userService.adminPaksiLogout(id);
        return ResponseEntity.ok(ApiResponse.sukses(null, "User berhasil dipaksa logout"));
    }

    @GetMapping("/shift")
    @Operation(
            summary = "Daftar shift",
            description = "Mengambil daftar shift aktif untuk pilihan saat membuat atau mengubah user"
    )
    public ResponseEntity<ApiResponse<List<ShiftResponse>>> daftarShift(
            @RequestParam(required = false) Long opdId
    ) {

        List<ShiftResponse> result =
                userService.daftarShift(opdId);

        return ResponseEntity.ok(
                ApiResponse.sukses(
                        result,
                        "Daftar shift"
                )
        );
    }
}
