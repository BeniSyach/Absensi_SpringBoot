package com.absensi.absensi_app.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ShiftRequest {

    @NotBlank(message = "Nama shift tidak boleh kosong")
    @Size(max = 100)
    private String nama;

    @NotNull(message = "Jam masuk wajib diisi")
    private LocalTime jamMasuk;

    @NotNull(message = "Jam pulang wajib diisi")
    private LocalTime jamPulang;

    @Min(value = 0, message = "Toleransi tidak boleh negatif")
    @Max(value = 120, message = "Toleransi maksimal 120 menit")
    private Integer toleransiTerlambat = 15;

    @Min(value = 0)
    @Max(value = 120)
    private Integer toleransiPulangAwal = 10;

    @NotNull(message = "OPD wajib dipilih")
    private Long opdId;

    /**
     * lintasHari akan dihitung otomatis di @PrePersist.
     * Field ini opsional — jika dikirim akan di-override oleh entity.
     */
    private Boolean lintasHari;
}