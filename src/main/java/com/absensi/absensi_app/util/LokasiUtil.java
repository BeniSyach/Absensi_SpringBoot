package com.absensi.absensi_app.util;

import com.absensi.absensi_app.dto.request.LokasiRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LokasiUtil {

    @Value("${app.lokasi.min-accuracy:50}")
    private float minAkurasi;

    @Value("${app.lokasi.max-mock-speed:50}")
    private double maxKecepatanWajar;

    private static final double EARTH_RADIUS = 6371000; // meter

    /**
     * Hitung jarak antara dua koordinat menggunakan Haversine formula
     * @return jarak dalam meter
     */
    public double hitungJarak(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    /**
     * Validasi apakah lokasi dalam radius kantor
     */
    public boolean isLokasiDalamRadius(double latUser, double lonUser,
                                       double latKantor, double lonKantor,
                                       int radiusKantor) {
        double jarak = hitungJarak(latUser, lonUser, latKantor, lonKantor);
        log.debug("Jarak dari kantor: {} meter, radius: {} meter", jarak, radiusKantor);
        return jarak <= radiusKantor;
    }

    /**
     * Deteksi potensi mock location berdasarkan berbagai kriteria:
     * 1. Akurasi GPS terlalu sempurna (tepat 0.0 meter - ciri khas emulator/mock)
     * 2. Provider bukan dari sumber terpercaya
     * 3. Koordinat tidak valid (out of range)
     * 4. Kecepatan perpindahan tidak wajar (teleportasi)
     */
    public HasilValidasiLokasi validasiLokasi(LokasiRequest request,
                                              Double latSebelumnya,
                                              Double lonSebelumnya,
                                              Long waktuSebelumnyaMs) {
        HasilValidasiLokasi hasil = new HasilValidasiLokasi();
        StringBuilder alasan = new StringBuilder();

        // 1. Cek koordinat valid
        if (!isKoordinatValid(request.getLatitude(), request.getLongitude())) {
            hasil.setMockTerdeteksi(true);
            alasan.append("Koordinat di luar batas valid. ");
        }

        // 2. Cek akurasi GPS
        if (request.getAkurasiGps() != null) {
            // Akurasi 0.0 persis atau minus adalah tanda mock
            if (request.getAkurasiGps() <= 0) {
                hasil.setMockTerdeteksi(true);
                alasan.append("Akurasi GPS tidak valid (0 atau negatif). ");
            }
            // Akurasi terlalu bagus (< 1 meter) mencurigakan kecuali provider fused
            if (request.getAkurasiGps() < 1.0 && !"fused".equalsIgnoreCase(request.getLocationProvider())) {
                hasil.setSuspect(true);
                alasan.append("Akurasi GPS mencurigakan (terlalu sempurna). ");
            }
        }

        // 3. Cek provider
        if (request.getLocationProvider() != null) {
            String provider = request.getLocationProvider().toLowerCase();
            // Provider tidak dikenal mencurigakan
            if (!provider.equals("gps") && !provider.equals("network")
                    && !provider.equals("fused") && !provider.equals("passive")) {
                hasil.setSuspect(true);
                alasan.append("Location provider tidak dikenal: " + provider + ". ");
            }
        } else {
            hasil.setSuspect(true);
            alasan.append("Location provider tidak tersedia. ");
        }

        // 4. Deteksi teleportasi (kecepatan tidak wajar)
        if (latSebelumnya != null && lonSebelumnya != null && waktuSebelumnyaMs != null) {
            double jarakPerpindahan = hitungJarak(
                    latSebelumnya, lonSebelumnya,
                    request.getLatitude(), request.getLongitude()
            );
            long selisihWaktuMs = System.currentTimeMillis() - waktuSebelumnyaMs;
            if (selisihWaktuMs > 0) {
                double kecepatanMs = jarakPerpindahan / (selisihWaktuMs / 1000.0);
                hasil.setKecepatanPerpindahan(kecepatanMs);

                if (kecepatanMs > maxKecepatanWajar) {
                    hasil.setMockTerdeteksi(true);
                    alasan.append(String.format(
                            "Kecepatan perpindahan tidak wajar: %.1f m/s. ", kecepatanMs));
                }
            }
        }

        // 5. Flag mock dari device (jika dikirim oleh app Android)
        if (Boolean.TRUE.equals(request.getIsMockLocation())) {
            hasil.setMockTerdeteksi(true);
            alasan.append("Device melaporkan mock location aktif. ");
        }

        hasil.setAlasan(alasan.toString().trim());
        return hasil;
    }

    private boolean isKoordinatValid(Double lat, Double lon) {
        if (lat == null || lon == null) return false;
        return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }

    public static class HasilValidasiLokasi {
        private boolean mockTerdeteksi = false;
        private boolean suspect = false;
        private String alasan = "";
        private Double kecepatanPerpindahan;

        public boolean isMockTerdeteksi() { return mockTerdeteksi; }
        public void setMockTerdeteksi(boolean v) { this.mockTerdeteksi = v; }
        public boolean isSuspect() { return suspect; }
        public void setSuspect(boolean v) { this.suspect = v; }
        public String getAlasan() { return alasan; }
        public void setAlasan(String v) { this.alasan = v; }
        public Double getKecepatanPerpindahan() { return kecepatanPerpindahan; }
        public void setKecepatanPerpindahan(Double v) { this.kecepatanPerpindahan = v; }
    }
}
