package com.absensi.absensi_app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisTokenService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.blacklist-prefix}")
    private String blacklistPrefix;

    @Value("${jwt.token-prefix}")
    private String tokenPrefix;

    /**
     * Simpan token aktif (access token) ke Redis
     * Key: jwt:token:{userId}:{deviceId} => token
     */
    public void simpanToken(Long userId, String deviceId, String token, long expiryMs) {
        String key = buildTokenKey(userId, deviceId);
        redisTemplate.opsForValue().set(key, token, expiryMs, TimeUnit.MILLISECONDS);
        log.debug("Token disimpan untuk user {} device {}", userId, deviceId);
    }

    /**
     * Ambil token aktif dari Redis
     */
    public String ambilToken(Long userId, String deviceId) {
        String key = buildTokenKey(userId, deviceId);
        Object val = redisTemplate.opsForValue().get(key);
        return val != null ? val.toString() : null;
    }

    /**
     * Hapus token (logout)
     */
    public void hapusToken(Long userId, String deviceId) {
        String key = buildTokenKey(userId, deviceId);
        redisTemplate.delete(key);
        log.debug("Token dihapus untuk user {} device {}", userId, deviceId);
    }

    /**
     * Blacklist token (misal setelah logout, token lama tidak bisa dipakai)
     */
    public void blacklistToken(String token, long expiryMs) {
        String key = blacklistPrefix + token;
        redisTemplate.opsForValue().set(key, "blacklisted", expiryMs, TimeUnit.MILLISECONDS);
        log.debug("Token di-blacklist");
    }

    /**
     * Cek apakah token masuk blacklist
     */
    public boolean isTokenBlacklisted(String token) {
        String key = blacklistPrefix + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Simpan data lokasi terakhir user untuk deteksi teleportasi
     * Expire setelah 1 jam (jika tidak absen lagi dianggap tidak relevan)
     */
    public void simpanLokasiTerakhir(Long userId, double lat, double lon, long timestampMs) {
        String key = "lokasi:terakhir:" + userId;
        String value = lat + "," + lon + "," + timestampMs;
        redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);
    }

    /**
     * Ambil lokasi terakhir user [lat, lon, timestamp]
     */
    public double[] ambilLokasiTerakhir(Long userId) {
        String key = "lokasi:terakhir:" + userId;
        Object val = redisTemplate.opsForValue().get(key);
        if (val == null) return null;
        String[] parts = val.toString().split(",");
        if (parts.length < 3) return null;
        return new double[]{
                Double.parseDouble(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2])
        };
    }

    /**
     * Rate limiting: cek dan tambah counter
     * Return true jika masih dalam batas
     */
    public boolean cekRateLimit(String key, int maxRequest, int periodDetik) {
        String redisKey = "ratelimit:" + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count == 1) {
            redisTemplate.expire(redisKey, periodDetik, TimeUnit.SECONDS);
        }
        return count <= maxRequest;
    }

    /**
     * Cache data dengan TTL
     */
    public void set(String key, Object value, long ttlMs) {
        redisTemplate.opsForValue().set(key, value, ttlMs, TimeUnit.MILLISECONDS);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    private String buildTokenKey(Long userId, String deviceId) {
        return tokenPrefix + userId + ":" + (deviceId != null ? deviceId : "default");
    }
}
