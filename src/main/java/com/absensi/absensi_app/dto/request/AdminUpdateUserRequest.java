package com.absensi.absensi_app.dto.request;

import com.absensi.absensi_app.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdminUpdateUserRequest {

    @NotBlank(message = "NIP tidak boleh kosong")
    @Size(min = 9, max = 20)
    private String nip;

    @NotBlank(message = "Nama lengkap tidak boleh kosong")
    @Size(min = 3, max = 100)
    private String namaLengkap;

    @Email(message = "Format email tidak valid")
    private String email;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Nomor telepon tidak valid")
    private String telepon;

    @NotNull(message = "OPD wajib dipilih")
    private Long opdId;

    private Role role;

    private Boolean aktif;

    // Bisa digunakan untuk memindah shift user
    private Long shiftId;
}
