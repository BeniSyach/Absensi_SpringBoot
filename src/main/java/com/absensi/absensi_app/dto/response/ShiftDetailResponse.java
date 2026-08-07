package com.absensi.absensi_app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ShiftDetailResponse {

    private Long id;
    private String nama;
    private Boolean aktif;

    private List<WaktuKerjaResponse> waktuKerja;
}