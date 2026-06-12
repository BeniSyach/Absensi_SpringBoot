package com.absensi.absensi_app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private Long id;
    private String nip;
    private String username;
    private String namaLengkap;
    private String email;
    private String telepon;
    private String fotoProfil;
    private String role;
    private OpdResponse opd;
}
