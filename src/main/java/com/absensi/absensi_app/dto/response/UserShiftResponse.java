package com.absensi.absensi_app.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserShiftResponse {

    private Long id;
    private String nip;
    private String username;
    private String namaLengkap;

    private Long shiftId;
    private String namaShift;
}