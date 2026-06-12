package com.absensi.absensi_app.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateProfilRequest {

    @NotBlank(message = "Nama lengkap tidak boleh kosong")
    @Size(min = 3, max = 100, message = "Nama lengkap harus 3-100 karakter")
    private String namaLengkap;

    @Email(message = "Format email tidak valid")
    private String email;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Nomor telepon tidak valid")
    private String telepon;
}
