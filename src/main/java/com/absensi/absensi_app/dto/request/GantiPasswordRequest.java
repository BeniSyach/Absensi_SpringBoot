package com.absensi.absensi_app.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class GantiPasswordRequest {

    @NotBlank(message = "Password lama tidak boleh kosong")
    private String passwordLama;

    @NotBlank(message = "Password baru tidak boleh kosong")
    @Size(min = 8, message = "Password baru minimal 8 karakter")
    private String passwordBaru;

    @NotBlank(message = "Konfirmasi password tidak boleh kosong")
    private String konfirmasiPasswordBaru;
}
