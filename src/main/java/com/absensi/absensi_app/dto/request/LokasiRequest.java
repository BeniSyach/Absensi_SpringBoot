package com.absensi.absensi_app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LokasiRequest {
    @NotNull(message = "Latitude tidak boleh kosong")
    private Double latitude;

    @NotNull(message = "Longitude tidak boleh kosong")
    private Double longitude;

    // Akurasi GPS dari device dalam meter
    private Float akurasiGps;

    // Provider lokasi: gps, network, fused, passive
    private String locationProvider;

    // Flag mock location dari Android (LocationManager.isFromMockProvider)
    private Boolean isMockLocation;

    // Altitude (opsional, untuk validasi tambahan)
    private Double altitude;

    // Bearing/arah (opsional)
    private Float bearing;

    // Kecepatan yang dilaporkan device
    private Float speed;
}