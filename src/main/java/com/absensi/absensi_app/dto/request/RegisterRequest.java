package com.absensi.absensi_app.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "NIP tidak boleh kosong")
    private String nip;

    @NotBlank(message = "Username tidak boleh kosong")
    @Size(min = 4, max = 50, message = "Username harus 4-50 karakter")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username hanya boleh huruf, angka, titik, underscore, dan strip")
    private String username;

    @NotBlank(message = "Password tidak boleh kosong")
    @Size(min = 8, message = "Password minimal 8 karakter")
    private String password;

    @NotBlank(message = "Konfirmasi password tidak boleh kosong")
    private String konfirmasiPassword;

    @NotBlank(message = "Nama lengkap tidak boleh kosong")
    @Size(min = 3, max = 100, message = "Nama lengkap harus 3-100 karakter")
    private String namaLengkap;

    @Email(message = "Format email tidak valid")
    private String email;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Nomor telepon tidak valid (10-15 digit angka)")
    private String telepon;

    @NotNull(message = "OPD wajib dipilih")
    private Long opdId;

    // Device ID perangkat Android
    private String deviceId;

    // Untuk registrasi mandiri, role default ROLE_USER
    // Admin bisa override via endpoint admin
}
