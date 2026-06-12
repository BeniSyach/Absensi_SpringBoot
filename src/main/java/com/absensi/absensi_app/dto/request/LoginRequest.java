package com.absensi.absensi_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username tidak boleh kosong")
    private String username;

    @NotBlank(message = "Password tidak boleh kosong")
    private String password;

    // Device ID untuk single device enforcement
    private String deviceId;

    // Info device untuk audit
    private String deviceInfo;
}