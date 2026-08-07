package com.absensi.absensi_app.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ShiftRequest {

    @NotBlank(message = "Nama shift tidak boleh kosong")
    @Size(max = 100)
    private String nama;

    @NotNull(message = "OPD wajib dipilih")
    private Long opdId;
}