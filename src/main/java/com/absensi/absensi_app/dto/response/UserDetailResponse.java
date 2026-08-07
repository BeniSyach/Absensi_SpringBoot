package com.absensi.absensi_app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDetailResponse {
    private Long id;
    private String nip;
    private String username;
    private String namaLengkap;
    private String email;
    private String telepon;
    private String fotoProfil;
    private String role;
    private Boolean aktif;
    private String deviceId;
    private OpdResponse opd;
    private ShiftResponse shiftAktif;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ShiftResponse shift;
}
