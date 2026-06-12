package com.absensi.absensi_app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Queue berbasis Redis untuk menyerap spike traffic jam absen.
 *
 * CARA KERJA:
 * 1. Request absen masuk → validasi cepat → data dimasukkan ke Redis queue
 * 2. Worker memproses queue dengan laju terkontrol → tulis ke PostgreSQL
 * 3. User langsung dapat response "absen diterima" tanpa nunggu DB
 *
 * Ini mirip pola "write-through queue" untuk menyerap spike.
 * Untuk produksi: ganti dengan RabbitMQ atau Kafka untuk durabilitas lebih baik.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AbsensiQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String QUEUE_MASUK  = "queue:absen:masuk";
    private static final String QUEUE_PULANG = "queue:absen:pulang";
    private static final String DLQ_MASUK    = "dlq:absen:masuk";   // Dead Letter Queue
    private static final String DLQ_PULANG   = "dlq:absen:pulang";
    private static final int    BATCH_SIZE   = 50;
    private static final int    MAX_RETRY    = 3;

    private final AtomicInteger processedCount = new AtomicInteger(0);

    /**
     * Tambahkan absen masuk ke queue
     */
    public void enqueueAbsenMasuk(AbsenPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForList().rightPush(QUEUE_MASUK, json);
            log.debug("Absen masuk di-queue untuk user: {}", payload.getUserId());
        } catch (JsonProcessingException e) {
            log.error("Gagal enqueue absen masuk: {}", e.getMessage());
            throw new RuntimeException("Gagal memproses absen");
        }
    }

    /**
     * Tambahkan absen pulang ke queue
     */
    public void enqueueAbsenPulang(AbsenPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForList().rightPush(QUEUE_PULANG, json);
        } catch (JsonProcessingException e) {
            log.error("Gagal enqueue absen pulang: {}", e.getMessage());
            throw new RuntimeException("Gagal memproses absen");
        }
    }

    /**
     * Ukuran queue saat ini
     */
    public long ukuranQueueMasuk() {
        Long size = redisTemplate.opsForList().size(QUEUE_MASUK);
        return size != null ? size : 0;
    }

    public long ukuranQueuePulang() {
        Long size = redisTemplate.opsForList().size(QUEUE_PULANG);
        return size != null ? size : 0;
    }

    /**
     * Ambil batch dari queue untuk diproses worker
     */
    public java.util.List<String> dequeueBatch(String queueKey, int batchSize) {
        java.util.List<String> batch = new java.util.ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            Object item = redisTemplate.opsForList().leftPop(queueKey);
            if (item == null) break;
            batch.add(item.toString());
        }
        return batch;
    }

    public String getMasukQueue()  { return QUEUE_MASUK; }
    public String getPulangQueue() { return QUEUE_PULANG; }
    public String getDlqMasuk()    { return DLQ_MASUK; }
    public String getDlqPulang()   { return DLQ_PULANG; }

    public void pindahKeDlq(String item, String dlqKey) {
        redisTemplate.opsForList().rightPush(dlqKey, item);
    }

    public int getBatchSize() { return BATCH_SIZE; }

    public void incrementProcessed() { processedCount.incrementAndGet(); }

    @Scheduled(fixedRate = 60000)
    public void logMetricsQueue() {
        long qMasuk  = ukuranQueueMasuk();
        long qPulang = ukuranQueuePulang();
        if (qMasuk > 0 || qPulang > 0) {
            log.info("Queue status — masuk: {}, pulang: {}, processed/menit: {}",
                    qMasuk, qPulang, processedCount.getAndSet(0));
        }
    }

    /**
     * DTO payload yang disimpan di queue
     */
    public static class AbsenPayload {
        public Long userId;
        public Long opdId;
        public Double latitude;
        public Double longitude;
        public Float akurasiGps;
        public String locationProvider;
        public Boolean isMockLocation;
        public Double kecepatanPerpindahan;
        public Boolean lokasiValid;
        public Boolean mockDetected;
        public Double jarakDariKantor;
        public String fotoPath;
        public String status;
        public String catatan;
        public String ipAddress;
        public String deviceInfo;
        public Long waktuMs;
        public int retryCount;

        public Long getUserId() { return userId; }
    }
}
