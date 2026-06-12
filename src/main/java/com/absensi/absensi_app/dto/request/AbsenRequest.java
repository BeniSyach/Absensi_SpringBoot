package com.absensi.absensi_app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AbsenRequest {

    @NotNull(message = "Data lokasi wajib diisi")
    @Valid
    private LokasiRequest lokasi;

    // Foto diupload sebagai multipart, disimpan terpisah
    // Field ini diisi setelah upload foto
    private String fotoToken;

    private String catatan;
}

